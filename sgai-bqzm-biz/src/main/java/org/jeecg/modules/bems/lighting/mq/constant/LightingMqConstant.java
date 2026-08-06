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
//    public static final String QUEUE_LIGHTING_LISTENER = "lighting_data";
    public static final String QUEUE_LIGHTING_LISTENER = "lighting_data_test";

    /**
     * 照明计划控制队列（延迟消息）
     */
    public static final String QUEUE_LIGHTING_PLAN = "bqzm_lighting_plan_execution";

    /**
     * 照明计划控制路由（延迟消息）
     */
    public static final String ROUTING_KEY_LIGHTING_PLAN = "bqzm_plan_execution";

    /**
     * 照明计划控制交换机（延迟消息）
     */
    public static final String EXCHANGE_LIGHTING_PLAN = "bqzm_lighting_plan_exchange";

    /**
     * 回路通讯状态队列
     */
    public static final String QUEUE_LIGHTING_CIRCUIT_COMSTAT = "bqzm_lighting_circuit_comstat";

    /**
     * 回路通讯状态路由
     */
    public static final String ROUTING_KEY_LIGHTING_COMSTAT = "bqzm_comstat_execution";

    /**
     * 四高炉灯控专用-小程序同步状态队列
     * 接收电箱控制小程序同步过来的泛光电箱状态
     */
    public static final String QUEUE_SGF_LIGHTING_STATUS_SYNC = "QUEUE_SGF_LIGHTING_STATUS_SYNC";

    /**
     * 泛光节目操作队列（发送到电箱控制小程序）
     */
    public static final String QUEUE_LIGHTING_GROUP_OPER = "QUEUE_LIGHTING_GROUP_OPER";

    public static final String QUEUE_ELECTRIC_BOX_OPERATION = "QUEUE_ELECTRIC_BOX_OPERATION";

    /**
     * 1号馆控制消息发送队列（通过MQ转发小程序发给181服务器）
     */
    public static final String QUEUE_LIGHTING_SEND_1HG = "lighting_control_1hg";

    /**
     * 1号馆状态反馈队列（接收MQ转发小程序从181服务器转过来的状态消息）
     */
    public static final String QUEUE_LIGHTING_LISTENER_1HG = "lighting_data_1hg";

}