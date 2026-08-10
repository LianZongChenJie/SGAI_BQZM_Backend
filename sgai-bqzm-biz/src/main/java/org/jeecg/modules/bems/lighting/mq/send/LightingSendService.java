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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class LightingSendService {

    private final RabbitTemplate rabbitTemplate;

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
     * 北区（space=903）网关编号 → 控制消息发送队列映射（11-44号网关）
     */
    private static final Map<String, String> BQ_CONTROL_QUEUE_MAP = new HashMap<>();
    static {
        BQ_CONTROL_QUEUE_MAP.put("11", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_11);
        BQ_CONTROL_QUEUE_MAP.put("12", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_12);
        BQ_CONTROL_QUEUE_MAP.put("13", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_13);
        BQ_CONTROL_QUEUE_MAP.put("14", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_14);
        BQ_CONTROL_QUEUE_MAP.put("15", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_15);
        BQ_CONTROL_QUEUE_MAP.put("16", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_16);
        BQ_CONTROL_QUEUE_MAP.put("17", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_17);
        BQ_CONTROL_QUEUE_MAP.put("18", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_18);
        BQ_CONTROL_QUEUE_MAP.put("19", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_19);
        BQ_CONTROL_QUEUE_MAP.put("20", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_20);
        BQ_CONTROL_QUEUE_MAP.put("21", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_21);
        BQ_CONTROL_QUEUE_MAP.put("22", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_22);
        BQ_CONTROL_QUEUE_MAP.put("23", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_23);
        BQ_CONTROL_QUEUE_MAP.put("24", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_24);
        BQ_CONTROL_QUEUE_MAP.put("25", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_25);
        BQ_CONTROL_QUEUE_MAP.put("26", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_26);
        BQ_CONTROL_QUEUE_MAP.put("27", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_27);
        BQ_CONTROL_QUEUE_MAP.put("28", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_28);
        BQ_CONTROL_QUEUE_MAP.put("29", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_29);
        BQ_CONTROL_QUEUE_MAP.put("30", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_30);
        BQ_CONTROL_QUEUE_MAP.put("31", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_31);
        BQ_CONTROL_QUEUE_MAP.put("32", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_32);
        BQ_CONTROL_QUEUE_MAP.put("33", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_33);
        BQ_CONTROL_QUEUE_MAP.put("34", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_34);
        BQ_CONTROL_QUEUE_MAP.put("35", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_35);
        BQ_CONTROL_QUEUE_MAP.put("36", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_36);
        BQ_CONTROL_QUEUE_MAP.put("37", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_37);
        BQ_CONTROL_QUEUE_MAP.put("38", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_38);
        BQ_CONTROL_QUEUE_MAP.put("39", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_39);
        BQ_CONTROL_QUEUE_MAP.put("40", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_40);
        BQ_CONTROL_QUEUE_MAP.put("41", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_41);
        BQ_CONTROL_QUEUE_MAP.put("42", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_42);
        BQ_CONTROL_QUEUE_MAP.put("43", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_43);
        BQ_CONTROL_QUEUE_MAP.put("44", LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_44);
    }

    /**
     * 发送北区（space=903）控制消息（通过MQ转发小程序发给181服务器）
     * 根据 circuit_code 前缀自动判断发到哪个队列：
     * 11-xxx → lighting_control_bq_11 队列，GatewayCode=11
     * ...
     * 44-xxx → lighting_control_bq_44 队列，GatewayCode=44
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

        // 确定队列（11-44号网关）
        String queueName = BQ_CONTROL_QUEUE_MAP.get(gatewayCode);
        if (queueName == null) {
            log.error("【北区】未知的网关编号，无法发送：gatewayCode={}, circuitCode={}", gatewayCode, circuitCode);
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