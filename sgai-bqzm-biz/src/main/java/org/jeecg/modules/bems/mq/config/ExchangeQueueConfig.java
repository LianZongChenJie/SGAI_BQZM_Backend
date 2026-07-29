package org.jeecg.modules.bems.mq.config;

import com.google.common.collect.ImmutableMap;
import org.jeecg.modules.bems.mq.constant.MqConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * mq交换机、队列配置
 */
@Configuration
public class ExchangeQueueConfig {

    /**
     * 计量规则点位更新队列
     */
    @Bean
    public Queue meteringPointDataUpdate() {
        return new Queue(MqConstant.QUEUE_METERING_POINT_DATA_UPDATE, true);
    }

    /**
     * 报警规则能耗变更报警队列
     */
    @Bean
    public Queue alarmAccumulate(){
        return new Queue(MqConstant.QUEUE_ALARM_ACCUMULATE,true);
    }

    /**
     * 报警规则属性变更报警队列
     */
    @Bean
    public Queue alarmInstantaneous(){
        return new Queue(MqConstant.QUEUE_ALARM_INSTANTANEOUS,true);
    }

    /**
     * 报警规则-计量规则点位值变更报警队列
     */
    @Bean
    public Queue alarmVirtual(){
        return new Queue(MqConstant.QUEUE_ALARM_VIRTUAL,true);
    }
    /**
     * 计量规则点位数据更新队列
     * @return
     */
    @Bean
    public Queue meteringPointValueUpdate(){
        return new Queue(MqConstant.QUEUE_METERING_POINT_VALUE_UPDATE,true);
    }

    /**
     * 成本点位数据更新
     */
    @Bean
    public Queue costPointDataUpdate(){
        return new Queue(MqConstant.COST_POINT_DATA_UPDATE,true);
    }

    /**
     * 计量表能耗变更DeviceStatusListener
     * 广播模式
     */
    @Bean
    public FanoutExchange energyConsumptionChange() {
        return new FanoutExchange(MqConstant.EXCHANGE_ENERGY_CONSUMPTION_CHANGE, true, false);
    }

    /**
     * 计量表能耗变更-计量规则点位更新
     */
    @Bean
    Binding bindingExchangeA() {
        return BindingBuilder.bind(meteringPointDataUpdate()).to(energyConsumptionChange());
    }

    /**
     * 计量表能耗变更-报警
     */
    @Bean
    Binding bindingExchangeB() {
        return BindingBuilder.bind(alarmAccumulate()).to(energyConsumptionChange());
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(MqConstant.EXCHANGE_DEAD_LETTER, true, false);
    }

    /**
     * 设备能耗数据采集死信队列
     */
    @Bean
    public Queue deadLetterEnergyDataGather() {
        return new Queue(MqConstant.QUEUE_DEAD_LETTER_DEVICE_ENERGY_DATA_GATHER, true);
    }

    /**
     * 死信队列绑定死信交换机
     */
    @Bean
    public Binding bindingDeadLetterExchangeA() {
        return BindingBuilder.bind(deadLetterEnergyDataGather()).to(deadLetterExchange()).with(MqConstant.QUEUE_DEAD_LETTER_DEVICE_ENERGY_DATA_GATHER);
    }

    /**
     * 设备能耗数据采集
     */
    @Bean
    public Queue deviceEnergyDataGather(){
        Map<String, Object> args = new HashMap<>();
        // 设置死信交换机
        args.put("x-dead-letter-exchange", MqConstant.EXCHANGE_DEAD_LETTER);
        // 设置死信路由键
        args.put("x-dead-letter-routing-key", MqConstant.QUEUE_DEAD_LETTER_DEVICE_ENERGY_DATA_GATHER);
        return new Queue(MqConstant.QUEUE_DEVICE_ENERGY_DATA_GATHER, true, false, false,args);
    }

    /**
     * 楼控点位数据采集
     */
    @Bean
    public Queue buildingControlPoint(){
        return new Queue(MqConstant.QUEUE_BUILDING_CONTROL_POINT,true);
    }

    /**
     * 设备能耗数据采集-日
     */
    @Bean
    public Queue deviceEnergyDataDayGather(){
        return new Queue(MqConstant.QUEUE_DEVICE_ENERGY_DATA_DAY_GATHER,true);
    }

    /**
     * 设备属性变更
     */
    @Bean
    public Queue deviceAttributeDataChange(){
        return new Queue(MqConstant.QUEUE_DEVICE_ATTRIBUTE_CHANGE,true);
    }

    /**
     * 设备最后采集时间
     */
    @Bean
    public Queue deviceLastGatherTime(){
        return new Queue(MqConstant.QUEUE_DEVICE_LAST_GATHER_TIME,true);
    }

    /**
     * 设备运行状态变更
     */
    @Bean
    public Queue deviceStatus(){
        return new Queue(MqConstant.QUEUE_DEVICE_RUN_STATUS_CHANGE,true);
    }

    /**
     * 设备数据延迟交换机
     */
    @Bean
    public CustomExchange deviceDataDelayExchange(){
        return new CustomExchange(
                MqConstant.EXCHANGE_DEVICE_DELAY,
                "x-delayed-message",
                true,
                false,
                ImmutableMap.of("x-delayed-type", "direct")
        );
    }

    /**
     * 设备状态队列绑定
     */
    @Bean
    public Binding deviceStatusBinding(){
        return BindingBuilder
                .bind(deviceStatus())
                .to(deviceDataDelayExchange())
                .with(MqConstant.ROUTING_KEY_DELAY_DEVICE_STATUS)
                .noargs();
    }

    /**
     * 计量点位数据变更统计交换机
     */
    @Bean
    public FanoutExchange meteringPointDataChange(){
        return new FanoutExchange(MqConstant.EXCHANGE_METERING_POINT_DATA_CHANGE, true, false);
    }

    /**
     * 告警条件判断
     */
    @Bean
    public Binding bindingMeteringPointDataChangeA(){
        return BindingBuilder.bind(alarmVirtual()).to(meteringPointDataChange());
    }

    /**
     * 成本计算
     */
    @Bean
    public Binding bindingMeteringPointDataChangeB(){
        return BindingBuilder.bind(costPointDataUpdate()).to(meteringPointDataChange());
    }

    /**
     * 楼控点位操作
     */
    @Bean
    public Queue buildingControlPointOperation(){
        return new Queue(MqConstant.QUEUE_BUILDING_CONTROL_OPERATION,true);
    }
}
