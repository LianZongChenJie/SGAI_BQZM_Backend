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
    public static final String QUEUE_LIGHTING_SEND_1HG = "Lighting_operations";

    /**
     * 1号馆状态反馈队列（接收MQ转发小程序从181服务器转过来的状态消息）
     */
    public static final String QUEUE_LIGHTING_LISTENER_1HG = "Lighting";

    /**
     * 北区（space=903）控制消息发送队列前缀，实际队列名 = 前缀 + 网关编号（如 lighting_control_bq_11）
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_PREFIX = "lighting_control_bq_";

    /**
     * 北区（space=903）控制消息发送队列 - 11号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_11 = "lighting_control_bq_11";

    /**
     * 北区（space=903）控制消息发送队列 - 12号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_12 = "lighting_control_bq_12";

    /**
     * 北区（space=903）控制消息发送队列 - 13号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_13 = "lighting_control_bq_13";

    /**
     * 北区（space=903）控制消息发送队列 - 14号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_14 = "lighting_control_bq_14";

    /**
     * 北区（space=903）控制消息发送队列 - 15号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_15 = "lighting_control_bq_15";

    /**
     * 北区（space=903）控制消息发送队列 - 16号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_16 = "lighting_control_bq_16";

    /**
     * 北区（space=903）控制消息发送队列 - 17号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_17 = "lighting_control_bq_17";

    /**
     * 北区（space=903）控制消息发送队列 - 18号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_18 = "lighting_control_bq_18";

    /**
     * 北区（space=903）控制消息发送队列 - 19号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_19 = "lighting_control_bq_19";

    /**
     * 北区（space=903）控制消息发送队列 - 20号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_20 = "lighting_control_bq_20";

    /**
     * 北区（space=903）控制消息发送队列 - 21号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_21 = "lighting_control_bq_21";

    /**
     * 北区（space=903）控制消息发送队列 - 22号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_22 = "lighting_control_bq_22";

    /**
     * 北区（space=903）控制消息发送队列 - 23号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_23 = "lighting_control_bq_23";

    /**
     * 北区（space=903）控制消息发送队列 - 24号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_24 = "lighting_control_bq_24";

    /**
     * 北区（space=903）控制消息发送队列 - 25号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_25 = "lighting_control_bq_25";

    /**
     * 北区（space=903）控制消息发送队列 - 26号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_26 = "lighting_control_bq_26";

    /**
     * 北区（space=903）控制消息发送队列 - 27号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_27 = "lighting_control_bq_27";

    /**
     * 北区（space=903）控制消息发送队列 - 28号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_28 = "lighting_control_bq_28";

    /**
     * 北区（space=903）控制消息发送队列 - 29号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_29 = "lighting_control_bq_29";

    /**
     * 北区（space=903）控制消息发送队列 - 30号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_30 = "lighting_control_bq_30";

    /**
     * 北区（space=903）控制消息发送队列 - 31号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_31 = "lighting_control_bq_31";

    /**
     * 北区（space=903）控制消息发送队列 - 32号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_32 = "lighting_control_bq_32";

    /**
     * 北区（space=903）控制消息发送队列 - 33号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_33 = "lighting_control_bq_33";

    /**
     * 北区（space=903）控制消息发送队列 - 34号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_34 = "lighting_control_bq_34";

    /**
     * 北区（space=903）控制消息发送队列 - 35号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_35 = "lighting_control_bq_35";

    /**
     * 北区（space=903）控制消息发送队列 - 36号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_36 = "lighting_control_bq_36";

    /**
     * 北区（space=903）控制消息发送队列 - 37号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_37 = "lighting_control_bq_37";

    /**
     * 北区（space=903）控制消息发送队列 - 38号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_38 = "lighting_control_bq_38";

    /**
     * 北区（space=903）控制消息发送队列 - 39号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_39 = "lighting_control_bq_39";

    /**
     * 北区（space=903）控制消息发送队列 - 40号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_40 = "lighting_control_bq_40";

    /**
     * 北区（space=903）控制消息发送队列 - 41号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_41 = "lighting_control_bq_41";

    /**
     * 北区（space=903）控制消息发送队列 - 42号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_42 = "lighting_control_bq_42";

    /**
     * 北区（space=903）控制消息发送队列 - 43号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_43 = "lighting_control_bq_43";

    /**
     * 北区（space=903）控制消息发送队列 - 44号网关
     */
    public static final String QUEUE_LIGHTING_SEND_BQ_44 = "lighting_control_bq_44";

    /**
     * 北区（space=903）状态反馈队列
     */
    public static final String QUEUE_LIGHTING_LISTENER_BQ = "lighting_data_bq";

}