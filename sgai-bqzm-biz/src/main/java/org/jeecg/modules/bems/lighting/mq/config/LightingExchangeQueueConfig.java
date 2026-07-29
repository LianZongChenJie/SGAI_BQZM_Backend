package org.jeecg.modules.bems.lighting.mq.config;

import com.google.common.collect.ImmutableMap;
import org.jeecg.modules.bems.lighting.mq.constant.LightingMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LightingExchangeQueueConfig {

    /**
     * 照明控制消息发送队列，四高炉
     */
    @Bean
    public Queue lightingSend() {
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND, true);
    }

    /**
     * 照明状态反馈队列
     */
    @Bean
    public Queue lightingListener(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_LISTENER,true);
    }

    /**
     * 照明控制消息发送队列，一高炉
     */
    @Bean
    public Queue lightingSendYgl(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_YGL,true);
    }

    /**
     * 照明计划交换机
     */
    @Bean
    public CustomExchange lightingPlanExchange(){
        return new CustomExchange(
                LightingMqConstant.EXCHANGE_LIGHTING_PLAN,
                "x-delayed-message",
                true,
                false,
                ImmutableMap.of("x-delayed-type", "direct")
                );
    }

    /**
     * 照明计划队列
     */
    @Bean
    public Queue lightingPlanQueue(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_PLAN,true);
    }

    /**
     * 回路通讯状态队列
     */
    @Bean
    public Queue lightingCircuitComstat(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_CIRCUIT_COMSTAT,true);
    }


    /**
     * 照明计划队列绑定
     */
    @Bean
    public Binding lightingPlanBinding(){
        return BindingBuilder
                .bind(lightingPlanQueue())
                .to(lightingPlanExchange())
                .with(LightingMqConstant.ROUTING_KEY_LIGHTING_PLAN)
                .noargs();
    }

    /**
     * 回路通讯状态队列绑定
     */
    @Bean
    public Binding lightingCircuitComstatBinding(){
        return BindingBuilder
                .bind(lightingCircuitComstat())
                .to(lightingPlanExchange())
                .with(LightingMqConstant.ROUTING_KEY_LIGHTING_COMSTAT)
                .noargs();
    }


}
