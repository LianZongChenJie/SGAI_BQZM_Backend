package org.jeecg.modules.bems.patterned.mq.config;

import org.jeecg.modules.bems.patterned.mq.constant.PatterningMqConstant;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 场景控制 RabbitMQ 配置
 */
@Configuration
public class PatterningMqConfig {

    /**
     * 场景控制延迟交换机 (x-delayed-message 类型)
     */
    @Bean
    public CustomExchange patterningDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(
                PatterningMqConstant.EXCHANGE_PATTERNING_DELAY,
                "x-delayed-message",
                true,
                false,
                args
        );
    }

    /**
     * 场景控制执行队列
     */
    @Bean
    public Queue patterningExecuteQueue() {
        return QueueBuilder.durable(PatterningMqConstant.QUEUE_PATTERNING_EXECUTE).build();
    }

    /**
     * 场景控制队列绑定延迟交换机
     */
    @Bean
    public Binding patterningExecuteBinding() {
        return BindingBuilder
                .bind(patterningExecuteQueue())
                .to(patterningDelayExchange())
                .with(PatterningMqConstant.ROUTING_KEY_PATTERNING_EXECUTE)
                .noargs();
    }
}
