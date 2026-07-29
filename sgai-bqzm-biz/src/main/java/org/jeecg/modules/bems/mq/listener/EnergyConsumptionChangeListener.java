package org.jeecg.modules.bems.mq.listener;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.annotation.RabbitComponent;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataService;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointRelService;
import org.jeecg.modules.bems.mq.constant.MqConstant;
import org.jeecg.modules.bems.mq.send.MqSendService;
import org.jeecg.modules.bems.mq.util.MeteringPointUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 设备能耗数据变更
 */
@RabbitComponent(value = "EnergyConsumptionChangeListener")
@Slf4j
@AllArgsConstructor
public class EnergyConsumptionChangeListener {
    private final IMeteringPointDataService meteringPointDataService;

    private final IMeteringPointRelService meteringPointRelService;

    private final MeteringPointUtil meteringPointUtil;

    private final MqSendService mqSendService;

    /**
     * 计量仪表能耗数据变更，获取影响的计量点位、发送计量点位数据更新消息
     * @param message
     */
    @RabbitListener(queues = MqConstant.QUEUE_METERING_POINT_DATA_UPDATE,ackMode = "AUTO")
    public void meteringPointDataUpdate(Message message){
        String body = new String(message.getBody());
        try {
            JSONObject jsonObject = JSONObject.parseObject(body);
            Long deviceId = jsonObject.getLong("deviceId");
            Date hour = jsonObject.getDate("hour");
            LocalDateTime hourTime = LocalDateTime.ofInstant(hour.toInstant(), ZoneId.systemDefault());
            // 获取设备关联点位信息
            List<Long> pointIds = meteringPointRelService.findPointIdsByDeviceId(deviceId);
            // 发送消息
            pointIds.forEach(pointId -> {
                mqSendService.sendMeteringPointValueUpdate(pointId,hourTime);
            });
        }catch (Exception e){
            log.error("mq消息消费失败。queue:{}，message:{}",MqConstant.QUEUE_METERING_POINT_DATA_UPDATE,body,e);
        }
    }

    /**
     * 设备计量点位数据更新
     * @param message
     */
    @RabbitListener(queues = MqConstant.QUEUE_METERING_POINT_VALUE_UPDATE,ackMode = "AUTO")
    public void meteringPointValueUpdate(Message message){
        String body = new String(message.getBody());
        try{
            JSONObject jsonObject = JSONObject.parseObject(body);
            Long pointId = jsonObject.getLong("meteringPointId");
            Date hour = jsonObject.getDate("hour");
            LocalDateTime time = LocalDateTime.ofInstant(hour.toInstant(), ZoneId.systemDefault());
            meteringPointUtil.clearMessageMark(pointId,time);
            meteringPointDataService.calculatePointValue(pointId, time);
        }catch (Exception e){
            log.error("mq消息消费失败。queue:{}，message:{}",MqConstant.QUEUE_METERING_POINT_VALUE_UPDATE,body,e);
        }
    }
}
