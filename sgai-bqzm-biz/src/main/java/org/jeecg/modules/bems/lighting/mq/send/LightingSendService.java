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
     * @param gatewayAdr 网关地址（Type=IpTunneling;HostAddress=xxx）
     * @param knxAdr KNX地址
     * @param value 值（100=开，0=关）
     */
    public void send1hgControl(String gatewayAdr, String knxAdr, String value) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("GatewayAdr", gatewayAdr);
        msg.put("KnxAdr", knxAdr);
        msg.put("value", value);
        msg.put("CollectionTime", LocalDateTimeUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        log.info("【1号馆】发送控制消息：GatewayAdr={}, KnxAdr={}, value={}", gatewayAdr, knxAdr, value);
        rabbitTemplate.send("", LightingMqConstant.QUEUE_LIGHTING_SEND_1HG, new Message(JSONObject.toJSONString(msg).getBytes(), properties));
    }

    /**
     * 发送北区（space=903）控制消息（通过MQ转发小程序发给181服务器）
     * 根据 circuit_code 前缀自动判断发到哪个队列：
     * 11-xxx → lighting_control_bq_11 队列，GatewayCode=11
     * 12-xxx → lighting_control_bq_12 队列，GatewayCode=12
     * @param circuitCode 回路编码（格式：11-20 或 12-20）
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

        // 确定队列
        String queueName;
        if ("11".equals(gatewayCode)) {
            queueName = LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_11;
        } else if ("12".equals(gatewayCode)) {
            queueName = LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_12;
        } else {
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
