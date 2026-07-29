package org.jeecg.modules.bems.mq.send;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.mq.constant.MqConstant;
import org.jeecg.modules.bems.mq.dto.DeviceLastGatherTimeMsg;
import org.jeecg.modules.bems.mq.message.EnergyConsumptionChangeMessage;
import org.jeecg.modules.bems.mq.util.MeteringPointUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * MQ发送服务
 */
@Component
@AllArgsConstructor
public class MqSendService {
    private static final Log log = LogFactory.getLog(MqSendService.class);
    private final RabbitTemplate rabbitTemplate;

    private final MeteringPointUtil meteringPointUtil;

    private final RedisUtil redisUtil;

    private static final long CACHE_TIME = 1000L * 100L;

    final TimedCache<String, String> timedCache = CacheUtil.newTimedCache(CACHE_TIME);

    public static final MessageProperties messageProperties = MessagePropertiesBuilder.newInstance()
            .setContentType(MessageProperties.CONTENT_TYPE_JSON)
            .build();

    /**
     * 能耗变更
     *
     * @param deviceId 设备id
     * @param hour     小时
     */
    public void sendEnergyConsumptionChange(Long deviceId, LocalDateTime hour) {
        EnergyConsumptionChangeMessage message = new EnergyConsumptionChangeMessage();
        message.setDeviceId(deviceId);
        message.setHour(hour.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        rabbitTemplate.convertAndSend(MqConstant.EXCHANGE_ENERGY_CONSUMPTION_CHANGE, "",message,item -> item);
    }

    /**
     * 计量点位数据变更
     * @param pointId 点位id
     * @param hour 小时
     * @param value 值
     */
    public void sendMeteringPointDataChange(Long pointId,LocalDateTime hour,BigDecimal value){
        if(pointId == null || hour == null || value == null){
            return;
        }
        Map<String,Object> msg = new HashMap<>();
        msg.put("meteringPointId",pointId);
        msg.put("time",hour.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        msg.put("value",value);
        rabbitTemplate.convertAndSend(MqConstant.EXCHANGE_METERING_POINT_DATA_CHANGE, "",msg,item -> item);
    }

    public void sendMeteringPointValueUpdate(Long pointId,LocalDateTime hour){
        if(pointId == null || hour == null){
            return;
        }
        // 判断数据是否存在队列中
        boolean b = meteringPointUtil.checkMessageExists(pointId,hour);
        if(!b){
            // 消息存在
            return;
        }
        Map<String,Object> msg = new HashMap<>();
        msg.put("meteringPointId",pointId);
        msg.put("hour",hour.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        rabbitTemplate.convertAndSend("", MqConstant.QUEUE_METERING_POINT_VALUE_UPDATE,msg,item -> item);
    }

    public void sendDeviceEnergyData(String deviceCode,String value,String time){
        Map<String,String> msg = new HashMap<>();
        msg.put("deviceCode",deviceCode);
        msg.put("time", time);
        msg.put("value",value);
        String jsonString = JSONObject.toJSONString(msg);
        Message message = new Message(jsonString.getBytes(), messageProperties);
        rabbitTemplate.convertAndSend("", MqConstant.QUEUE_DEVICE_ENERGY_DATA_GATHER, message);
    }

    public void sendDeviceAttributeValueChange(Long deviceId,Long pointId,String value){
        Map<String,Object> msg = new HashMap<>();
        msg.put("deviceId",deviceId);
        msg.put("pointId",pointId);
        msg.put("value",value);
        String jsonString = JSONObject.toJSONString(msg);
        Message message = new Message(jsonString.getBytes(), messageProperties);
        rabbitTemplate.convertAndSend("", MqConstant.QUEUE_ALARM_INSTANTANEOUS, message);
    }

    /**
     * 设备离线状态延迟推送
     * @param deviceCode 设备状态
     * @param delayTime 延迟时间，秒
     */
    public void sendDeviceOfflineDelay(String deviceCode,long delayTime){
        Map<String,Object> msg = new HashMap<>();
        msg.put("deviceCode",deviceCode);
        msg.put("status","离线");
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("x-delay",delayTime * 1000L);
        // 设置缓存key
        redisUtil.set("device_status:" + deviceCode,"在线",delayTime - 10L);
        rabbitTemplate.send(MqConstant.EXCHANGE_DEVICE_DELAY, MqConstant.ROUTING_KEY_DELAY_DEVICE_STATUS, new Message(JSONObject.toJSONString(msg).getBytes(), properties));
    }

    /**
     * 设备最后采集时间
     * @param deviceCode 设备编号
     * @param time 通讯时间
     * @param offLineTime 离线间隔时间
     */
    public void sendDeviceLastGatherTime(String deviceCode,LocalDateTime time,LocalDateTime offLineTime){
        String key = deviceCode + ":" + time.getMinute();
        String lock = timedCache.get(key);
        if(lock != null){
            return;
        }
        timedCache.put(key,"value");
        DeviceLastGatherTimeMsg msg = new DeviceLastGatherTimeMsg();
        msg.setDeviceCode(deviceCode);
        msg.setTime(time);
        msg.setOffLineTime(offLineTime);
        rabbitTemplate.send("",MqConstant.QUEUE_DEVICE_LAST_GATHER_TIME,new Message(JSONObject.toJSONString(msg).getBytes(), messageProperties));
    }

    /**
     * 楼控点位控制
     * @param gatewayAdr 网关地址
     * @param bacnetAdr 点位地址
     * @param value 值
     */
    public void sendBuildingControlOperation(String gatewayAdr,String bacnetAdr,String value){
        Map<String,Object> msg = new HashMap<>();
        msg.put("GatewayAdr",gatewayAdr);
        msg.put("BacnetAdr",bacnetAdr);
        msg.put("Value",value);
        String jsonString = JSONObject.toJSONString(msg);
        Message message = new Message(jsonString.getBytes(), messageProperties);
        rabbitTemplate.convertAndSend("", MqConstant.QUEUE_BUILDING_CONTROL_OPERATION, message);
    }

}
