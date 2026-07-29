package org.jeecg.modules.bems.mq.listener;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.annotation.RabbitComponent;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.mdm.dto.DeviceStatusDto;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mq.constant.MqConstant;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * 设备状态监听
 */
@RabbitComponent(value = "DeviceStatusListener")
@Slf4j
@AllArgsConstructor
public class DeviceStatusListener {

    private final IDeviceService deviceService;

    private final RedisUtil redisUtil;

    @RabbitListener(queues = MqConstant.QUEUE_DEVICE_RUN_STATUS_CHANGE, ackMode = "AUTO")
    @RabbitHandler
    public void listener(Message message) {
        String body = new String(message.getBody());
        try {
            JSONObject jsonObject = JSONObject.parseObject(body);
            String deviceCode = jsonObject.getString("deviceCode");
            String status = jsonObject.getString("status");
            if(StrUtil.isAllEmpty(deviceCode,status)){
                return;
            }
            Object key = redisUtil.get("device_status:" + deviceCode);
            if(key == null) {
                deviceService.updateStatus(deviceCode, status);
            }
        } catch (Exception e) {
            log.error("mq消息消费失败。queue:{}，message:{}", MqConstant.QUEUE_DEVICE_RUN_STATUS_CHANGE, body, e);
        }
    }
}
