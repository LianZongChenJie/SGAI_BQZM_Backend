package org.jeecg.modules.bems.lighting.mq.send;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.lighting.mq.constant.LightingMqConstant;
import org.jeecg.modules.bems.lighting.mq.message.LightInfoUpdateLoad;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class LightingSendService {

    private final RabbitTemplate rabbitTemplate;

    private final RedisUtil redisUtil;

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
     * @param version 计划版本号
     * @param executionTime 执行时间
     */
    public void sendPlan(Long planId,String version, LocalDateTime executionTime){
        Map<String,Object> msg = new HashMap<>();
        msg.put("planId",planId);
        msg.put("version",version);
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        long delayTime = getDelayTime(executionTime);
        if(delayTime < 0){
            log.error("计划执行时间已过，执行失败。计划id：{}",planId);
        }
        properties.setHeader("x-delay",delayTime * 1000L);
        log.info("照明计划发送消息：{}", msg);
        rabbitTemplate.send(LightingMqConstant.EXCHANGE_LIGHTING_PLAN, LightingMqConstant.ROUTING_KEY_LIGHTING_PLAN, new Message(JSONObject.toJSONString(msg).getBytes(), properties));
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
        log.info("发送泛光节目操作消息：groupId={}, onOff={}, sceneId={}, sceneName={}", groupId, onOff, sceneId, sceneName);
        rabbitTemplate.convertAndSend("", LightingMqConstant.QUEUE_LIGHTING_GROUP_OPER, JSONObject.toJSONString(msg));
    }

}