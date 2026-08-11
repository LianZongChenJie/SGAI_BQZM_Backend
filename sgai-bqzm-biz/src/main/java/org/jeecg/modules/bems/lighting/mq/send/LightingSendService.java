package org.jeecg.modules.bems.lighting.mq.send;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson.JSONObject;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
        switch (gatewayCode) {
            case "1":
                rabbitTemplate.convertAndSend("", LightingMqConstant.QUEUE_LIGHTING_SEND, msg);
                break;
            case "2":
                rabbitTemplate.convertAndSend("", LightingMqConstant.QUEUE_LIGHTING_SEND_YGL, msg);
                break;
            case "4":
                rabbitTemplate.convertAndSend("",LightingMqConstant.QUEUE_LIGHTING_SEND_DTT,msg);
                break;
            case "3":
                rabbitTemplate.convertAndSend("",LightingMqConstant.QUEUE_LIGHTING_SEND_039,msg);
                break;
            default:
                log.error("发送消息失败,无法区分区域：{}", msg);
                break;
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
        long delayTime = 1000L * 60L * 20L;
        properties.setHeader("x-delay",delayTime);
        rabbitTemplate.send(LightingMqConstant.EXCHANGE_LIGHTING_PLAN,LightingMqConstant.ROUTING_KEY_LIGHTING_COMSTAT,new Message(JSONObject.toJSONString(msg).getBytes(), properties));
        redisUtil.set("lighting:" + space + ":" + areaCode + ":" + circuitCode, "离线", delayTime - 1000L);
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
     * 已确认存在（或已自动创建）的北区控制队列缓存，避免每次发送都去 MQ 检查
     */
    private static final Set<String> BQ_QUEUE_READY_CACHE = ConcurrentHashMap.newKeySet();

    /**
     * 获取北区控制队列名（前缀 + 网关编号），队列不存在时自动创建（幂等）
     * @param gatewayCode 网关编号（纯数字）
     * @return 队列名，检查/创建失败返回 null
     */
    private String getOrCreateBqQueue(String gatewayCode) {
        String queueName = LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_PREFIX + gatewayCode;
        if (BQ_QUEUE_READY_CACHE.contains(queueName)) {
            return queueName;
        }
        synchronized (BQ_QUEUE_READY_CACHE) {
            if (BQ_QUEUE_READY_CACHE.contains(queueName)) {
                return queueName;
            }
            try {
                // 队列不存在才创建，已存在（属性一致）则跳过，避免重复声明
                if (rabbitAdmin.getQueueInfo(queueName) == null) {
                    String declared = rabbitAdmin.declareQueue(new Queue(queueName, true));
                    if (declared == null) {
                        log.error("【北区】自动创建队列失败：queue={}", queueName);
                        return null;
                    }
                    log.info("【北区】自动创建控制队列：queue={}", queueName);
                }
                BQ_QUEUE_READY_CACHE.add(queueName);
                return queueName;
            } catch (Exception e) {
                log.error("【北区】检查/创建队列失败：queue={}, error={}", queueName, e.getMessage(), e);
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
        String queueName = getOrCreateBqQueue(gatewayCode);
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

}