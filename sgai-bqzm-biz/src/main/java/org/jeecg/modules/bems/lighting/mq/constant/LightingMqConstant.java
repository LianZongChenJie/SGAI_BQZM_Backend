package org.jeecg.modules.bems.lighting.mq.constant;

public class LightingMqConstant {

    /**
     * 照明控制消息发送队列，金安桥
     */
    public static final String QUEUE_LIGHTING_SEND = "lighting_control";

    /**
     * 照明控制消息发送队列，一高炉
     */
    public static final String QUEUE_LIGHTING_SEND_YGL = "lighting_control_ygl";

    /**
     * 照明控制消息发送队列，大跳台
     */
    public static final String QUEUE_LIGHTING_SEND_DTT = "lighting_control_dtt";

    /**
     * 照明控制消息发送队列，039
     */
    public static final String QUEUE_LIGHTING_SEND_039 = "lighting_control_039";

    /**
     * 照明状态反馈队列
     */
    public static final String QUEUE_LIGHTING_LISTENER = "lighting_data";

    /**
     * 照明计划控制队列（延迟消息）
     */
    public static final String QUEUE_LIGHTING_PLAN = "lighting_plan_execution";

    /**
     * 照明计划控制路由（延迟消息）
     */
    public static final String ROUTING_KEY_LIGHTING_PLAN = "plan_execution";

    /**
     * 照明计划控制交换机（延迟消息）
     */
    public static final String EXCHANGE_LIGHTING_PLAN = "lighting_plan_exchange";

    /**
     * 回路通讯状态队列
     */
    public static final String QUEUE_LIGHTING_CIRCUIT_COMSTAT = "lighting_circuit_comstat";

    /**
     * 回路通讯状态路由
     */
    public static final String ROUTING_KEY_LIGHTING_COMSTAT = "comstat_execution";

}
