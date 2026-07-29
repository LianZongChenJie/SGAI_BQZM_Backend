package org.jeecg.modules.bems.mq.constant;

public class MqConstant {
    /**
     * 设备能源数据采集
     */
    public static final String QUEUE_DEVICE_ENERGY_DATA_GATHER = "device_energy-data_gather";

    /**
     * 设备能源数据采集-日
     */
    public static final String QUEUE_DEVICE_ENERGY_DATA_DAY_GATHER = "device_energy-data_day_gather";

    /**
     * 设备运行状态变更队列
     */
    public static final String QUEUE_DEVICE_RUN_STATUS_CHANGE = "device_run_status_change";

    /**
     * 计量点位更新队列
     * 计量仪表能耗数据变更，消息数据：设备id、时间、能耗值
     * 获取设备关联的计量点位，发送计量点位数据更新消息
     */
    public static final String QUEUE_METERING_POINT_DATA_UPDATE = "MeteringPointDataUpdate";

    /**
     * 成本点位数据更新队列
     */
    public static final String COST_POINT_DATA_UPDATE = "CostPointDataUpdate";

    /**
     * 计量设备能耗数据变更交换机
     */
    public static final String EXCHANGE_ENERGY_CONSUMPTION_CHANGE = "EnergyConsumptionChange";

    /**
     * 计量点位数据更新队列
     * 消息数据：计量点位id、时间
     */
    public static final String QUEUE_METERING_POINT_VALUE_UPDATE = "MeteringPointValueUpdate";

    /**
     * 死信交换机
     */
    public static final String EXCHANGE_DEAD_LETTER = "dlx.exchange";

    /**
     * 设备能耗数据采集-死信队列
     */
    public static final String QUEUE_DEAD_LETTER_DEVICE_ENERGY_DATA_GATHER = "dlx.device_energy-data_gather";

    /**
     * 设备能耗变更报警队列
     */
    public static final String QUEUE_ALARM_ACCUMULATE = "alarm.accumulate";

    /**
     * 设备属性值变更报警队列
     */
    public static final String QUEUE_ALARM_INSTANTANEOUS = "alarm.instantaneous";

    /**
     * 计量规则点位数据变更报警队列
     */
    public static final String QUEUE_ALARM_VIRTUAL = "alarm.virtual";

    /**
     * 楼控点位推送
     */
    public static final String QUEUE_BUILDING_CONTROL_POINT = "bc_data";

    /**
     * 设备属性变更消息队列
     */
    public static final String QUEUE_DEVICE_ATTRIBUTE_CHANGE = "device_attribute_data_gather";

    /**
     * 设备最后采集时间
     */
    public static final String QUEUE_DEVICE_LAST_GATHER_TIME = "device_last_gather_time";

    /**
     * 设备数据延迟交换机
     */
    public static final String EXCHANGE_DEVICE_DELAY = "device_data_delay";

    /**
     * routing_key 设备状态队列
     */
    public static final String ROUTING_KEY_DELAY_DEVICE_STATUS = "device_data_delay_status";

    /**
     * 计量规则点位数据变化通知交换机
     */
    public static final String EXCHANGE_METERING_POINT_DATA_CHANGE = "metering_point_data_change";

    /**
     * 楼控点位控制队列
     */
    public static final String QUEUE_BUILDING_CONTROL_OPERATION = "bc_control";
}
