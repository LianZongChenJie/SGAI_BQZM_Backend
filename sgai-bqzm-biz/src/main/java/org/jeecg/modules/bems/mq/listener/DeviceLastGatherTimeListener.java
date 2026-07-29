package org.jeecg.modules.bems.mq.listener;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.annotation.RabbitComponent;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mq.constant.MqConstant;
import org.jeecg.modules.bems.mq.dto.DeviceLastGatherTimeMsg;
import org.jeecg.modules.bems.mq.send.MqSendService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 设备最后采集时间监听
 */
@RabbitComponent(value = "DeviceLastGatherTimeListener")
@Slf4j
@AllArgsConstructor
public class DeviceLastGatherTimeListener {

    private final IDeviceService deviceService;

    private final MqSendService mqSendService;

    @RabbitListener(queues = MqConstant.QUEUE_DEVICE_LAST_GATHER_TIME, ackMode = "AUTO")
    public void listener(Message message){
        String body = new String(message.getBody());
        try{
            DeviceLastGatherTimeMsg msg = JSONObject.parseObject(body, DeviceLastGatherTimeMsg.class);
            deviceService.updateLastGatherTime(msg.getDeviceCode(),msg.getTime());
            // 判断设备通讯状态
            LocalDateTime offLineTime = msg.getOffLineTime();
            LocalDateTime now = LocalDateTime.now();
            long between = ChronoUnit.SECONDS.between(now, offLineTime);
            if(between > 0){
                deviceService.updateStatus(msg.getDeviceCode(),"在线");
                // 发送延迟消息并设置缓存
                mqSendService.sendDeviceOfflineDelay(msg.getDeviceCode(),between);
            }else{
                deviceService.updateStatus(msg.getDeviceCode(),"离线");
            }
        }catch (Exception e){
            log.error("mq消息消费失败。queue:{},message:{}", MqConstant.QUEUE_DEVICE_LAST_GATHER_TIME, body, e);
        }
    }
}
