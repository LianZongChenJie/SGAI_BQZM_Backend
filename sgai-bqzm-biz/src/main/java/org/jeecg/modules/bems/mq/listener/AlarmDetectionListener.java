package org.jeecg.modules.bems.mq.listener;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.annotation.RabbitComponent;
import org.jeecg.modules.bems.alarm.service.IAlarmRecordService;
import org.jeecg.modules.bems.mq.constant.MqConstant;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 报警检测
 */
@RabbitComponent(value = "AlarmDetectionListener")
@Slf4j
@AllArgsConstructor
public class AlarmDetectionListener {

    private final IAlarmRecordService alarmRecordService;

    @RabbitListener(queues = MqConstant.QUEUE_ALARM_ACCUMULATE, ackMode = "AUTO")
    public void alarmAccumulate(Message message){
        String body = new String(message.getBody());
        try {
            JSONObject jsonObject = JSONObject.parseObject(body);
            Long deviceId = jsonObject.getLong("deviceId");
            Date hour = jsonObject.getDate("hour");
            LocalDateTime hourTime = LocalDateTime.ofInstant(hour.toInstant(), ZoneId.systemDefault());
            alarmRecordService.alarmDetection(deviceId,hourTime);
        } catch (Exception e) {
            log.error("mq消息消费失败。queue:{}，message:{}", MqConstant.QUEUE_ALARM_ACCUMULATE, body, e);
        }

    }

    @RabbitListener(queues = MqConstant.QUEUE_ALARM_VIRTUAL,ackMode = "AUTO")
    public void alarmVirtual(Message message){
        String body = new String(message.getBody());
        try {
            JSONObject jsonObject = JSONObject.parseObject(body);
            Long meteringPointId = jsonObject.getLong("meteringPointId");
            Date hour = jsonObject.getDate("time");
            LocalDateTime hourTime = LocalDateTime.ofInstant(hour.toInstant(), ZoneId.systemDefault());
            alarmRecordService.alarmDetectionForMeteringPoint(meteringPointId,hourTime);
        }catch (Exception e){
            log.error("mq消息消费失败。queue:{}，message:{}", MqConstant.QUEUE_ALARM_VIRTUAL, body, e);
        }
    }

    @RabbitListener(queues = MqConstant.QUEUE_ALARM_INSTANTANEOUS,ackMode = "AUTO")
    public void alarmInstantaneous(Message message){
        String body = new String(message.getBody());
        try {
            JSONObject jsonObject = JSONObject.parseObject(body);
            alarmRecordService.alarmDetection(jsonObject.getLong("deviceId"),jsonObject.getLong("pointId"),jsonObject.getString("value"));
        }catch (Exception e){
            log.error("mq消息消费失败。queue:{}，message:{}", MqConstant.QUEUE_ALARM_INSTANTANEOUS, body, e);
        }
    }

}
