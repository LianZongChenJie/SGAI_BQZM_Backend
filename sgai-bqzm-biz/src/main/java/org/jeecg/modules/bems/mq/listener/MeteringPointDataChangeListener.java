package org.jeecg.modules.bems.mq.listener;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.annotation.RabbitComponent;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataHour;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointCostDataService;
import org.jeecg.modules.bems.mq.constant.MqConstant;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * 计量规则点位值变更
 */
@RabbitComponent(value = "MeteringPointDataChangeListener")
@Slf4j
@AllArgsConstructor
public class MeteringPointDataChangeListener {

    private final IMeteringPointCostDataService meteringPointCostDataService;


    @RabbitListener(queues = MqConstant.COST_POINT_DATA_UPDATE,ackMode = "AUTO")
    public void meteringPointCostDataUpdate(Message message){
        String body = new String(message.getBody());
        try {
            MeteringPointDataHour dataHour = JSONObject.parseObject(body, MeteringPointDataHour.class);
            meteringPointCostDataService.calculationCost(dataHour.getMeteringPointId(),dataHour.getTime(),dataHour.getValue());
        }catch (Exception e){
            log.error("mq消息消费失败。queue:{}，message:{}",MqConstant.COST_POINT_DATA_UPDATE,body,e);
        }
    }
}
