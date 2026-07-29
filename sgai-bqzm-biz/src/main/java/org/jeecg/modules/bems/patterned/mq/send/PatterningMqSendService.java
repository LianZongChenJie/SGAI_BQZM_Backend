package org.jeecg.modules.bems.patterned.mq.send;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.patterned.dto.PatterningDelayMessage;
import org.jeecg.modules.bems.patterned.mq.constant.PatterningMqConstant;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 场景控制 MQ 发送服务
 */
@Component
@Slf4j
@AllArgsConstructor
public class PatterningMqSendService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送场景控制延迟消息
     * @param patterningId 场景控制ID
     * @param version 版本号
     * @param executeTime 执行时间
     */
    public void sendPatterningDelayMessage(Long patterningId, String version, LocalDateTime executeTime) {
        long delaySeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), executeTime);
        if (delaySeconds <= 0) {
            log.warn("场景控制延迟时间无效, patterningId={}, executeTime={}", patterningId, executeTime);
            return;
        }

        PatterningDelayMessage message = new PatterningDelayMessage(patterningId, executeTime, version);

        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("x-delay", delaySeconds * 1000L);

        String json = JSONObject.toJSONString(message);
        Message msg = new Message(json.getBytes(), properties);

        rabbitTemplate.send(
                PatterningMqConstant.EXCHANGE_PATTERNING_DELAY,
                PatterningMqConstant.ROUTING_KEY_PATTERNING_EXECUTE,
                msg
        );

        log.info("场景控制延迟消息已发送, patterningId={}, executeTime={}, delaySeconds={}",
                patterningId, executeTime, delaySeconds);
    }
}
