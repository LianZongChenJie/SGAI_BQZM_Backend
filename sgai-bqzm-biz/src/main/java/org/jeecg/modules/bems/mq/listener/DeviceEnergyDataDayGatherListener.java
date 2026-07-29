package org.jeecg.modules.bems.mq.listener;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.annotation.RabbitComponent;
import org.jeecg.modules.bems.mq.constant.MqConstant;
import org.jeecg.modules.bems.service.IDeviceDataService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 计量数据采集-日数据
 * 不计算小时、分钟数据，只计算日、月、年数据
 */
@RabbitComponent(value = "DeviceEnergyDataDayGatherListener")
@Slf4j
@AllArgsConstructor
public class DeviceEnergyDataDayGatherListener {

    private final IDeviceDataService deviceDataService;

    @RabbitListener(queues = MqConstant.QUEUE_DEVICE_ENERGY_DATA_DAY_GATHER, ackMode = "AUTO")
    public void deviceEnergyDayListener(Message message){
        String body = new String(message.getBody());
        try{
            JSONObject jsonObject = JSONObject.parseObject(body);
            String deviceCode = jsonObject.getString("deviceCode");
            String time = jsonObject.getString("time");
            String value = jsonObject.getString("value");
            deviceDataService.calculateValueDay(deviceCode, LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), new BigDecimal(value));
        }catch (Exception e){
            log.error("mq消息消费失败，queue：{}，message：{}", MqConstant.QUEUE_DEVICE_ENERGY_DATA_DAY_GATHER,body, e);
        }

    }


}
