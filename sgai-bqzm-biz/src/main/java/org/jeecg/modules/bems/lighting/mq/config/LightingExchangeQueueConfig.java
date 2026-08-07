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
     * 照明控制消息发送队列，金安桥
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
     * 四高炉操作队列
     */
    @Bean
    public Queue lightingListenersgl(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_GROUP_OPER,true);
    }

    /**
     * 照明控制消息发送队列，一高炉  节目
     */
    @Bean
    public Queue lightingSendYgl(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_YGL,true);
    }

    @Bean
    public Queue lightingSendSglare(){
        return new Queue(LightingMqConstant.QUEUE_ELECTRIC_BOX_OPERATION,true);
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
     * 四高炉灯控专用-小程序同步状态队列
     */
    @Bean
    public Queue sgfLightingStatusSync(){
        return new Queue(LightingMqConstant.QUEUE_SGF_LIGHTING_STATUS_SYNC,true);
    }

    /**
     * 1号馆控制消息发送队列（通过MQ转发小程序发给181服务器）
     */
    @Bean
    public Queue lightingSend1hg(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_1HG,true);
    }

    /**
     * 1号馆状态反馈队列（接收MQ转发小程序从181服务器转过来的状态消息）
     */
    @Bean
    public Queue lightingListener1hg(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_LISTENER_1HG,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 11号网关
     */
    @Bean
    public Queue lightingSendBq11(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_11,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 12号网关
     */
    @Bean
    public Queue lightingSendBq12(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_12,true);
    }

    /**
     * 北区（space=903）状态反馈队列
     */
    @Bean
    public Queue lightingListenerBq(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_LISTENER_BQ,true);
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