package org.jeecg.modules.bems.mq.listener;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.boot.starter.lock.client.RedissonLockClient;
import org.jeecg.common.annotation.RabbitComponent;
import org.jeecg.modules.bems.mq.constant.MqConstant;
import org.jeecg.modules.bems.service.IDeviceDataService;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 计量数据采集
 */
@RabbitComponent(value = "DeviceEnergyDataGatherListener")
@Slf4j
@AllArgsConstructor
public class DeviceEnergyDataGatherListener {

    private final IDeviceDataService deviceDataService;

    private final RedissonLockClient redissonLockClient;


    @RabbitListener(queues = MqConstant.QUEUE_DEVICE_ENERGY_DATA_GATHER, ackMode = "MANUAL", concurrency = "3-5")
    public void energyDataCollection(Message message, Channel channel) throws IOException {
        RetryTemplate retryTemplate = getRetryTemplate();

        try {
            retryTemplate.execute(context -> {
                String body = new String(message.getBody());
                try {
                    JSONObject jsonObject = JSON.parseObject(body);
                    String equipmentCode = jsonObject.getString("deviceCode");
                    String collectionTime = jsonObject.getString("time");
                    String collectionValue = jsonObject.getString("value");
                    processing(equipmentCode, collectionTime, collectionValue);
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    return null;
                } catch (Exception e) {
                    log.error("mq消息消费失败，queue：{}，message：{}", MqConstant.QUEUE_DEVICE_ENERGY_DATA_GATHER,body, e);
                    throw e; // 抛出异常触发重试
                }
            });
        } catch (Exception e) {
            // 重试次数耗尽后的处理
            log.error("mq消息消费失败，已达到最大重试次数。queue:{}，message:{}",
                    MqConstant.QUEUE_DEVICE_ENERGY_DATA_GATHER, new String(message.getBody()), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }

    private static @NotNull RetryTemplate getRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        // 配置重试策略
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(1.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        // 配置重试次数
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        return retryTemplate;
    }

    @RabbitListener(queues = MqConstant.QUEUE_DEAD_LETTER_DEVICE_ENERGY_DATA_GATHER, ackMode = "AUTO")
    public void dlqx(Message message, Channel channel) throws IOException {
        String body = new String(message.getBody());
        log.error("mq消息消费失败。queue:{}，message:{}", MqConstant.QUEUE_DEVICE_ENERGY_DATA_GATHER, body);
    }

    private void processing(String deviceCode, String time, String value) {
        boolean b = redissonLockClient.tryLock(getLockKey(deviceCode), 10, 60);
        if (b) {
            try {
                deviceDataService.calculateValue(deviceCode, LocalDateTimeUtil.parse(time, "yyyy-MM-dd HH:mm:ss"), new BigDecimal(value));
            } finally {
                redissonLockClient.unlock(getLockKey(deviceCode));
            }
        }
    }

    private String getLockKey(String deviceCode) {
        return "lock:device:data:gather" + deviceCode;
    }

}
