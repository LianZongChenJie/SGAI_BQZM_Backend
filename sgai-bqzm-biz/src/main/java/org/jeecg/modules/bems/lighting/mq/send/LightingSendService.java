package org.jeecg.modules.bems.lighting.mq.send;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.GetResponse;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecuteLog;
import org.jeecg.modules.bems.lighting.mq.constant.LightingMqConstant;
import org.jeecg.modules.bems.lighting.mq.message.LightInfoUpdateLoad;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecuteLogService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Service
@AllArgsConstructor
@Slf4j
public class LightingSendService {

    private final RabbitTemplate rabbitTemplate;

    private final RabbitAdmin rabbitAdmin;

    private final RedisUtil redisUtil;
    private final ProcessorMetrics processorMetrics;

    private final ILightingPlanExecuteLogService planExecuteLogService;

    public void send(LightInfoUpdateLoad msg) {
        log.info("发送消息：{}", msg);
        String gatewayCode = msg.getGatewayCode();
        if (StringUtils.isEmpty(gatewayCode)) {
            log.error("发送消息失败,无法区分区域：{}", msg);
            return;
        }
        String queueName = resolveOldSpaceQueue(gatewayCode);
        if (queueName == null) {
            log.error("发送消息失败,无法区分区域：{}", msg);
            return;
        }
        // 队列不存在时自动创建（持久化、幂等），防止消息静默丢失
        if (getOrCreateQueue(queueName) == null) {
            return;
        }
        rabbitTemplate.convertAndSend("", queueName, msg);
    }

    /**
     * 老空间（金安桥/一高炉/039/大跳台）区域控制队列（与 LightingAreaServiceImpl.resolveSpaceQueue 保持一致）
     */
    private String resolveOldSpaceQueue(String gatewayCode) {
        switch (gatewayCode) {
            case "1":
                return LightingMqConstant.QUEUE_LIGHTING_SEND;      // 金安桥
            case "2":
                return LightingMqConstant.QUEUE_LIGHTING_SEND_YGL;  // 一高炉
            case "3":
                return LightingMqConstant.QUEUE_LIGHTING_SEND_039;  // 039
            case "4":
                return LightingMqConstant.QUEUE_LIGHTING_SEND_DTT;  // 大跳台
            default:
                return null;
        }
    }

    /**
     * 发送照明计划执行消息
     * @param planId 计划id
     * @param planName 计划名称
     * @param version 计划版本号
     * @param executionTime 执行时间
     */
    public void sendPlan(Long planId,String planName, String version, LocalDateTime executionTime){
        Map<String,Object> msg = new HashMap<>();
        msg.put("planId",planId);
        msg.put("version",version);
        String executeDate = executionTime != null
                ? executionTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        msg.put("executeDate", executeDate);
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        long delayTime = getDelayTime(executionTime);
        if(delayTime < 0){
            log.error("计划执行时间已过，执行失败。计划id：{}",planId);
            // 执行时间已过，消息不会按时消费，直接记为失败
            planExecuteLogService.markConsumed(planId, version, executeDate, false, "计划执行时间已过，未发送延迟消息");
            return;
        }
        properties.setHeader("x-delay",delayTime * 1000L);
        log.info("照明计划发送消息：{}", msg);
        rabbitTemplate.send(LightingMqConstant.EXCHANGE_LIGHTING_PLAN, LightingMqConstant.ROUTING_KEY_LIGHTING_PLAN, new Message(JSONObject.toJSONString(msg).getBytes(), properties));
        // 记录执行日志（待消费状态），供日历展示 执行成功/执行失败
        planExecuteLogService.recordSend(planId, planName, version, executionTime);
    }

    public void sendLightingCircuitComstat(String space,String areaCode,String circuitCode){
        Map<String,Object> msg = new HashMap<>();
        msg.put("space",space);
        msg.put("areaCode",areaCode);
        msg.put("circuitCode",circuitCode);
        msg.put("comstat","离线");
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        // 延迟 20 分钟；x-delay 单位是毫秒，Redis TTL 单位是秒
        long delayMillis = 1000L * 60L * 20L;
        long delaySeconds = 60L * 20L;
        properties.setHeader("x-delay", delayMillis);
        rabbitTemplate.send(LightingMqConstant.EXCHANGE_LIGHTING_PLAN,LightingMqConstant.ROUTING_KEY_LIGHTING_COMSTAT,new Message(JSONObject.toJSONString(msg).getBytes(), properties));
        // Redis key 作为"设备 20 分钟内有上报"的标记，TTL 设为 20 分钟-1 秒，保证延迟消息消费时恰好过期
        redisUtil.set("lighting:" + space + ":" + areaCode + ":" + circuitCode, "离线", delaySeconds - 1L);
    }

    /**
     * 计算延迟时间
     * @param executionTime 计划执行时间
     * @return 延迟时间 秒
     */
    private long getDelayTime(LocalDateTime executionTime){
        return LocalDateTimeUtil.between(LocalDateTime.now(),executionTime, ChronoUnit.SECONDS);
    }

    /**
     * 发送泛光节目操作消息（到电箱控制小程序）
     * @param groupId 泛光节目ID
     * @param onOff 操作：1=开，2=关
     * @param sceneId 场景ID
     * @param sceneName 场景名称
     */
    public void sendGroupOper(String groupId, int onOff, Long sceneId, String sceneName) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("groupId", groupId);
        msg.put("onOff", onOff);
        msg.put("sceneId", sceneId);
        msg.put("sceneName", sceneName);
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        log.info("发送泛光节目操作消息：groupId={}, onOff={}, sceneId={}, sceneName={}", groupId, onOff, sceneId, sceneName);
        rabbitTemplate.send("", LightingMqConstant.QUEUE_LIGHTING_GROUP_OPER, new Message(JSONObject.toJSONString(msg).getBytes(),properties));
    }

    /**
     * 发送四高炉电箱控制消息（到电箱控制小程序）
     * 四高炉区域（space=901）的 area_code 即电箱设备编号 deviceSn，如 yel_power_sg06
     * @param deviceSn 电箱设备编号（= lighting_area.area_code）
     * @param onOff 操作：1=开，0=关
     */
    public void sendSgfControl(String deviceSn, String onOff) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("deviceSn", deviceSn);
        msg.put("onOff", onOff);
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        log.info("【四高炉】发送电箱控制消息：deviceSn={}, onOff={}", deviceSn, onOff);
        rabbitTemplate.send("", LightingMqConstant.QUEUE_ELECTRIC_BOX_OPERATION, new Message(JSONObject.toJSONString(msg).getBytes(), properties));
    }

    /**
     * 发送1号馆控制消息（通过MQ转发小程序发给181服务器）
     * @param circuitCode 完整的回路编码（格式：Type=IpTunneling;HostAddress=10.22.133.32-4.1.25-3/7/18）
     * @param value 值（100=开，0=关，内部自动转成true/false）
     */
    public void send1hgControl(String circuitCode, String value) {
        // 从 circuitCode 中提取纯IP（HostAddress=后面的部分）
        String ip = "";
        if (circuitCode != null && circuitCode.contains("HostAddress=")) {
            int start = circuitCode.indexOf("HostAddress=") + "HostAddress=".length();
            int end = circuitCode.indexOf('-', start);
            if (end < 0) {
                end = circuitCode.length();
            }
            ip = circuitCode.substring(start, end);
        }

        // 提取最后一个 "-" 后面的作为 KnxAdr
        String knxAdr = "";
        if (circuitCode != null) {
            int lastDash = circuitCode.lastIndexOf('-');
            if (lastDash >= 0 && lastDash < circuitCode.length() - 1) {
                knxAdr = circuitCode.substring(lastDash + 1);
            }
        }

        // value 转换：100→true，0→false
        String boolValue = "100".equals(value) ? "true" : "false";

        Map<String, Object> msg = new HashMap<>();
        msg.put("GatewayAdr", ip);
        msg.put("KnxAdr", knxAdr);
        msg.put("value", boolValue);

        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        log.info("【1号馆】发送控制消息：GatewayAdr={}, KnxAdr={}, value={}", ip, knxAdr, boolValue);
        rabbitTemplate.send("", LightingMqConstant.QUEUE_LIGHTING_SEND_1HG, new Message(JSONObject.toJSONString(msg).getBytes(), properties));
        log.info("【1号馆】发送控制消息：{}", JSONObject.toJSONString(msg));
    }

    /**
     * 已确认存在（或已自动创建）的队列缓存，避免每次发送都去 MQ 检查
     */
    private static final Set<String> QUEUE_READY_CACHE = ConcurrentHashMap.newKeySet();

    /**
     * 获取队列名，队列不存在时自动创建（持久化、幂等）
     * @param queueName 队列名
     * @return 队列名，检查/创建失败返回 null
     */
    private String getOrCreateQueue(String queueName) {
        if (QUEUE_READY_CACHE.contains(queueName)) {
            return queueName;
        }
        synchronized (QUEUE_READY_CACHE) {
            if (QUEUE_READY_CACHE.contains(queueName)) {
                return queueName;
            }
            try {
                // 队列不存在才创建，已存在（属性一致）则跳过，避免重复声明
                if (rabbitAdmin.getQueueInfo(queueName) == null) {
                    String declared = rabbitAdmin.declareQueue(new Queue(queueName, true));
                    if (declared == null) {
                        log.error("【MQ】自动创建队列失败：queue={}", queueName);
                        return null;
                    }
                    log.info("【MQ】自动创建控制队列：queue={}", queueName);
                }
                QUEUE_READY_CACHE.add(queueName);
                return queueName;
            } catch (Exception e) {
                log.error("【MQ】检查/创建队列失败：queue={}, error={}", queueName, e.getMessage(), e);
                return null;
            }
        }
    }

    /**
     * 发送北区（space=903）控制消息（通过MQ转发小程序发给181服务器）
     * 队列名 = 公共前缀 + 网关编号（如 11-xxx → lighting_control_bq_11，44-xxx → lighting_control_bq_44），
     * 不限制 11-44，任意网关编号都会拼出对应队列，队列不存在时自动创建
     * @param circuitCode 回路编码（格式：11-20 或 44-20）
     * @param value 值（100=开，0=关）
     */
    public void sendBqControl(String circuitCode, String value) {
        // 拆分 circuit_code：第一个 "-" 前面是 GatewayCode，后面是 CircuitCode
        int dashIndex = circuitCode.indexOf('-');
        if (dashIndex <= 0 || dashIndex >= circuitCode.length() - 1) {
            log.error("【北区】回路编码格式不对，无法发送：circuitCode={}", circuitCode);
            return;
        }
        String gatewayCode = circuitCode.substring(0, dashIndex);
        String code = circuitCode.substring(dashIndex + 1);

        // 网关编号必须是纯数字，防止拼出非法队列名
        if (!gatewayCode.matches("\\d+")) {
            log.error("【北区】网关编号格式不对，无法发送：gatewayCode={}, circuitCode={}", gatewayCode, circuitCode);
            return;
        }

        // 队列名 = 前缀 + 网关编号，不存在时自动创建
        String queueName = getOrCreateQueue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_PREFIX + gatewayCode);
        if (queueName == null) {
            return;
        }

        // 构造消息
        Map<String, Object> msg = new HashMap<>();
        msg.put("DataType", "0");
        msg.put("CircuitCode", code);
        msg.put("Value", value);
        msg.put("GatewayCode", gatewayCode);

        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        log.info("【北区】发送控制消息：queue={}, gatewayCode={}, circuitCode={}, value={}", queueName, gatewayCode, code, value);
        log.info("【北区】发送控制消息：{}", JSONObject.toJSONString(msg));
        rabbitTemplate.send("", queueName, new Message(JSONObject.toJSONString(msg).getBytes(), properties));
    }

    /**
     * 发送904空间（新灯控）控制消息（通过MQ转发小程序发给181服务器）
     * 消息格式：GatewayCode固定54；DataType 3=区域、0=回路；AreaID=区域编码（area_code）；
     * 回路控制消息需带 CircuitCode（回路号），区域控制不带。
     * 队列：lighting_control_bq_54（不存在时自动创建）
     * @param areaCode 区域编码（lighting_area.area_code）
     * @param circuitCode 回路号（区域控制传null）
     * @param value 值（回路：100=开、0=关；区域/场景：1=开、12=关）
     */
    public void send904Control(String areaCode, String circuitCode, String value) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("GatewayCode", "54");
        msg.put("Value", value);
        msg.put("AreaID", areaCode);
        boolean circuit = StringUtils.isNotEmpty(circuitCode);
        msg.put("DataType", circuit ? "0" : "3");
        if (circuit) {
            msg.put("CircuitCode", circuitCode);
        }
        String queueName = getOrCreateQueue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_54);
        if (queueName == null) {
            return;
        }
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        log.info("【904】发送控制消息：{}", JSONObject.toJSONString(msg));
        rabbitTemplate.send("", queueName, new Message(JSONObject.toJSONString(msg).getBytes(), properties));
    }

    /**
     * 发送905空间（新灯控）区域控制消息（通过MQ转发小程序发给181服务器）
     * 消息格式：{"DataType":"0","AreaID":"8","Value":"0","GatewayCode":"154.100"}
     * AreaID=区域下回路编码（lighting_circuit.circuit_code）；Value：0=开、1=关；
     * GatewayCode 固定 154.100。
     * 队列：lighting_control_bq_154_100（不存在时自动创建）
     * @param areaCode 回路编码（lighting_circuit.circuit_code，905 区域下唯一回路的编码）
     * @param value 值（0=开、1=关）
     */
    public void send905Control(String areaCode, String value) {
        if (StringUtils.isEmpty(areaCode)) {
            log.error("【905】区域编码为空，无法发送控制消息");
            return;
        }
        Map<String, Object> msg = new HashMap<>();
        msg.put("DataType", "0");
        msg.put("AreaID", areaCode);
        msg.put("Value", value);
        msg.put("GatewayCode", "154.100");
        String queueName = getOrCreateQueue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_905);
        if (queueName == null) {
            return;
        }
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        log.info("【905】发送控制消息：{}", JSONObject.toJSONString(msg));
        rabbitTemplate.send("", queueName, new Message(JSONObject.toJSONString(msg).getBytes(), properties));
    }

    /**
     * 发送906空间（新灯控）区域控制消息（通过MQ转发小程序发给181服务器）
     * 消息格式：{"DataType":"0","AreaID":"2","Value":"1","GatewayCode":"154.2"}
     * AreaID=区域下回路编码（lighting_circuit.circuit_code）；Value：1=开、2=关；
     * GatewayCode 固定 154.2。
     * 队列：lighting_control_bq_154_2（不存在时自动创建）
     * @param areaCode 回路编码（lighting_circuit.circuit_code，906 区域下唯一回路的编码）
     * @param value 值（1=开、2=关）
     */
    public void send906Control(String areaCode, String value) {
        if (StringUtils.isEmpty(areaCode)) {
            log.error("【906】区域编码为空，无法发送控制消息");
            return;
        }
        Map<String, Object> msg = new HashMap<>();
        msg.put("DataType", "0");
        msg.put("AreaID", areaCode);
        msg.put("Value", value);
        msg.put("GatewayCode", "154.2");
        String queueName = getOrCreateQueue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_906);
        if (queueName == null) {
            return;
        }
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        log.info("【906】发送控制消息：{}", JSONObject.toJSONString(msg));
        rabbitTemplate.send("", queueName, new Message(JSONObject.toJSONString(msg).getBytes(), properties));
    }

    /**
     * 队列扫描锁：防止同一队列的并发扫描互相干扰（listPage1 统计与撤回共用同一把锁）
     */
    private static final Map<String, Object> QUEUE_SCAN_LOCKS = new ConcurrentHashMap<>();

    /**
     * 队列扫描结果缓存：TTL 内同一队列只扫描一次。
     * listPage1 统计待下发消息数时，一页内多个同空间区域会反复调用 countQueueMessages
     * （如1号馆所有区域共用 Lighting_operations 队列），若每次都对同一队列全量 basicGet/放回扫描，
     * 队列消息多时会严重超时，故按队列缓存扫描结果，仅对缓存的 body 列表按条件过滤。
     */
    private static final Map<String, ScanCacheEntry> QUEUE_SCAN_CACHE = new ConcurrentHashMap<>();
    /** 统计缓存有效期（毫秒）：列表展示的待下发消息数为近似值，容忍短时间延迟 */
    private static final long SCAN_CACHE_TTL = 5000L;

    /** 队列扫描缓存项 */
    private static class ScanCacheEntry {
        final long scanTime;
        final List<byte[]> bodies;
        ScanCacheEntry(long scanTime, List<byte[]> bodies) {
            this.scanTime = scanTime;
            this.bodies = bodies;
        }
    }

    /**
     * 统计队列中未被消费且命中 condition 的消息条数：
     * 逐条取出队列头消息检查，不删除任何消息，全部按原相对顺序放回队列。
     * 用于按区域精确统计待下发消息数（共享队列只统计本区域的消息）。
     * 同一队列 TTL 内只真实扫描一次（结果缓存），其余调用直接在缓存 body 列表上按条件过滤，避免重复扫描拖垮接口。
     * @param queueName 队列名
     * @param condition 消息体匹配条件，null 时不执行
     * @return 命中条数，执行失败返回0
     */
    public int countQueueMessages(String queueName, Predicate<byte[]> condition) {
        if (condition == null) {
            return 0;
        }
        Object lock = QUEUE_SCAN_LOCKS.computeIfAbsent(queueName, k -> new Object());
        synchronized (lock) {
            // 缓存命中：直接在缓存的 body 列表上按条件过滤，不再重复扫描队列
            ScanCacheEntry cache = QUEUE_SCAN_CACHE.get(queueName);
            if (cache != null && System.currentTimeMillis() - cache.scanTime < SCAN_CACHE_TTL) {
                int matched = 0;
                for (byte[] body : cache.bodies) {
                    if (condition.test(body)) {
                        matched++;
                    }
                }
                return matched;
            }
            // 缓存过期或缺失：扫描队列一次并填充缓存
            List<byte[]> bodies = scanQueueBodies(queueName);
            if (bodies == null) {
                return 0;
            }
            // 清理过期缓存，避免 Map 无限增长
            QUEUE_SCAN_CACHE.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue().scanTime >= SCAN_CACHE_TTL);
            QUEUE_SCAN_CACHE.put(queueName, new ScanCacheEntry(System.currentTimeMillis(), bodies));
            int matched = 0;
            for (byte[] body : bodies) {
                if (condition.test(body)) {
                    matched++;
                }
            }
            return matched;
        }
    }

    /**
     * 扫描队列中未被消费的所有消息，收集 body 后全部放回队列（保持原相对顺序）。
     * 仅做统计采集，不删除任何消息；配合 QUEUE_SCAN_CACHE 缓存避免同一队列重复扫描。
     * @param queueName 队列名
     * @return 消息 body 列表；队列不存在或扫描异常返回 null
     */
    private List<byte[]> scanQueueBodies(String queueName) {
        // 队列不存在时跳过，避免 basicGet 触发 404 NOT_FOUND 通道错误
        try {
            if (rabbitAdmin.getQueueInfo(queueName) == null) {
                log.warn("【MQ扫描】队列不存在，跳过统计：queue={}", queueName);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.warn("【MQ扫描】检查队列失败，跳过统计：queue={}, error={}", queueName, e.getMessage());
            return new ArrayList<>();
        }
        List<byte[]> bodies = new ArrayList<>();
        try {
            rabbitTemplate.execute(channel -> {
                List<Long> toRequeue = new ArrayList<>();
                int scanned = 0;
                final int MAX_SCAN = 5000;
                while (scanned < MAX_SCAN) {
                    GetResponse response = channel.basicGet(queueName, false);
                    if (response == null) {
                        break;
                    }
                    scanned++;
                    bodies.add(response.getBody());
                    toRequeue.add(response.getEnvelope().getDeliveryTag());
                }
                // 未删除的消息放回队列，倒序放回保持原相对顺序
                for (int i = toRequeue.size() - 1; i >= 0; i--) {
                    channel.basicNack(toRequeue.get(i), false, true);
                }
                if (scanned >= MAX_SCAN) {
                    log.warn("【MQ扫描】队列消息较多，本次最多扫描 {} 条：queue={}", MAX_SCAN, queueName);
                }
                return null;
            });
        } catch (Exception e) {
            log.error("【MQ扫描】扫描队列失败：queue={}, error={}", queueName, e.getMessage(), e);
            return null;
        }
        return bodies;
    }

    /**
     * 按条件选择性撤回（删除）队列中未被消费的消息：
     * 逐条取出队列头消息，body 匹配 condition 的 ack 删除，不匹配的放回队列（保持相对顺序），
     * 返回删除条数。用于按区域撤回——共享队列（如北区同网关、1号馆、老空间）只删本区域的消息，不动其他区域。
     * @param queueName 队列名
     * @param condition 消息体匹配条件（匹配则删除），null 时不执行
     * @return 撤回（删除）的消息数，执行失败返回0
     */
    public int recallQueueMessages(String queueName, Predicate<byte[]> condition) {
        return scanQueueMessages(queueName, condition, true);
    }

    /**
     * 扫描队列中未被消费的消息：命中 condition 的计数（deleteMatched=true 时同时 ack 删除），
     * 未删除的消息全部放回队列（倒序放回保持原相对顺序）。
     */
    private int scanQueueMessages(String queueName, Predicate<byte[]> condition, boolean deleteMatched) {
        if (condition == null) {
            return 0;
        }
        // 队列不存在时跳过，避免 basicGet 触发 404 NOT_FOUND 通道错误
        try {
            if (rabbitAdmin.getQueueInfo(queueName) == null) {
                log.warn("【MQ扫描】队列不存在，跳过统计/撤回：queue={}", queueName);
                return 0;
            }
        } catch (Exception e) {
            log.warn("【MQ扫描】检查队列失败，跳过统计/撤回：queue={}, error={}", queueName, e.getMessage());
            return 0;
        }
        AtomicInteger matched = new AtomicInteger(0);
        Object lock = QUEUE_SCAN_LOCKS.computeIfAbsent(queueName, k -> new Object());
        synchronized (lock) {
            try {
                rabbitTemplate.execute(channel -> {
                    List<Long> toRequeue = new ArrayList<>();
                    int scanned = 0;
                    final int MAX_SCAN = 5000;
                    while (scanned < MAX_SCAN) {
                        GetResponse response = channel.basicGet(queueName, false);
                        if (response == null) {
                            break;
                        }
                        scanned++;
                        boolean hit = condition.test(response.getBody());
                        if (hit) {
                            matched.incrementAndGet();
                        }
                        if (deleteMatched && hit) {
                            channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
                        } else {
                            toRequeue.add(response.getEnvelope().getDeliveryTag());
                        }
                    }
                    // 未删除的消息放回队列，倒序放回保持原相对顺序
                    for (int i = toRequeue.size() - 1; i >= 0; i--) {
                        channel.basicNack(toRequeue.get(i), false, true);
                    }
                    if (scanned >= MAX_SCAN) {
                        log.warn("【MQ扫描】队列消息较多，本次最多扫描 {} 条：queue={}", MAX_SCAN, queueName);
                    }
                    return null;
                });
            } catch (Exception e) {
                log.error("【MQ扫描】扫描队列失败：queue={}, error={}", queueName, e.getMessage(), e);
            }
        }
        return matched.get();
    }

}