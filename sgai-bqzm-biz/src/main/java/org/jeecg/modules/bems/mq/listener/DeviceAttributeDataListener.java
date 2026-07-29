package org.jeecg.modules.bems.mq.listener;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.annotation.RabbitComponent;
import org.jeecg.modules.bems.mdm.dto.DeviceAttributeData;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeService;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mq.constant.MqConstant;
import org.jeecg.modules.bems.mq.send.MqSendService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * 设备采集点位监听
 */
@RabbitComponent(value = "DeviceAttributeDataListener")
@Slf4j
@AllArgsConstructor
public class DeviceAttributeDataListener {

    private final IDeviceAttributeService deviceAttributeService;

    private final IDeviceService deviceService;

    @RabbitListener(queues = MqConstant.QUEUE_DEVICE_ATTRIBUTE_CHANGE, ackMode = "AUTO")
    public void listener(Message message) {
        String body = new String(message.getBody());
        try {
            DeviceAttributeData data = JSONObject.parseObject(body, DeviceAttributeData.class);
            Device byDeviceCode = deviceService.getByDeviceCode(data.getEquipmentCode());
            if (byDeviceCode == null) {
                log.error("设备不存在：{}", data.getEquipmentCode());
                return;
            }
            // 更新点位信息
            deviceAttributeService.updateAttributeValue(byDeviceCode.getId(), data);
        } catch (Exception e) {
            log.error("mq消息消费失败。queue:{},message:{}", MqConstant.QUEUE_DEVICE_ATTRIBUTE_CHANGE, body, e);
        }
    }
}
