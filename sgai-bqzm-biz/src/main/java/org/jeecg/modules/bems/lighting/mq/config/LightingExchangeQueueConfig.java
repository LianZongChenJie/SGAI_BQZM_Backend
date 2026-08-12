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

    /**
     * 照明控制消息发送队列，039
     */
    @Bean
    public Queue lightingSend039(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_039,true);
    }

    /**
     * 照明控制消息发送队列，大跳台
     */
    @Bean
    public Queue lightingSendDtt(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_DTT,true);
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
     * 北区（space=903）控制消息发送队列 - 13号网关
     */
    @Bean
    public Queue lightingSendBq13(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_13,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 14号网关
     */
    @Bean
    public Queue lightingSendBq14(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_14,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 15号网关
     */
    @Bean
    public Queue lightingSendBq15(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_15,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 16号网关
     */
    @Bean
    public Queue lightingSendBq16(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_16,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 17号网关
     */
    @Bean
    public Queue lightingSendBq17(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_17,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 18号网关
     */
    @Bean
    public Queue lightingSendBq18(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_18,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 19号网关
     */
    @Bean
    public Queue lightingSendBq19(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_19,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 20号网关
     */
    @Bean
    public Queue lightingSendBq20(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_20,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 21号网关
     */
    @Bean
    public Queue lightingSendBq21(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_21,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 22号网关
     */
    @Bean
    public Queue lightingSendBq22(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_22,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 23号网关
     */
    @Bean
    public Queue lightingSendBq23(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_23,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 24号网关
     */
    @Bean
    public Queue lightingSendBq24(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_24,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 25号网关
     */
    @Bean
    public Queue lightingSendBq25(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_25,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 26号网关
     */
    @Bean
    public Queue lightingSendBq26(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_26,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 27号网关
     */
    @Bean
    public Queue lightingSendBq27(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_27,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 28号网关
     */
    @Bean
    public Queue lightingSendBq28(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_28,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 29号网关
     */
    @Bean
    public Queue lightingSendBq29(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_29,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 30号网关
     */
    @Bean
    public Queue lightingSendBq30(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_30,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 31号网关
     */
    @Bean
    public Queue lightingSendBq31(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_31,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 32号网关
     */
    @Bean
    public Queue lightingSendBq32(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_32,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 33号网关
     */
    @Bean
    public Queue lightingSendBq33(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_33,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 34号网关
     */
    @Bean
    public Queue lightingSendBq34(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_34,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 35号网关
     */
    @Bean
    public Queue lightingSendBq35(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_35,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 36号网关
     */
    @Bean
    public Queue lightingSendBq36(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_36,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 37号网关
     */
    @Bean
    public Queue lightingSendBq37(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_37,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 38号网关
     */
    @Bean
    public Queue lightingSendBq38(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_38,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 39号网关
     */
    @Bean
    public Queue lightingSendBq39(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_39,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 40号网关
     */
    @Bean
    public Queue lightingSendBq40(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_40,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 41号网关
     */
    @Bean
    public Queue lightingSendBq41(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_41,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 42号网关
     */
    @Bean
    public Queue lightingSendBq42(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_42,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 43号网关
     */
    @Bean
    public Queue lightingSendBq43(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_43,true);
    }

    /**
     * 北区（space=903）控制消息发送队列 - 44号网关
     */
    @Bean
    public Queue lightingSendBq44(){
        return new Queue(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_44,true);
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