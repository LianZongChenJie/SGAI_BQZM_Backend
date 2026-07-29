package org.jeecg.modules.bems.patterned.mq.constant;

/**
 * 场景控制 MQ 常量
 */
public class PatterningMqConstant {

    /**
     * 场景控制延迟交换机
     */
    public static final String EXCHANGE_PATTERNING_DELAY = "patterning.strategy.delay.exchange";

    /**
     * 场景控制执行队列
     */
    public static final String QUEUE_PATTERNING_EXECUTE = "patterning.strategy.execute.queue";

    /**
     * 场景控制路由键
     */
    public static final String ROUTING_KEY_PATTERNING_EXECUTE = "patterning.strategy.execute";
}
