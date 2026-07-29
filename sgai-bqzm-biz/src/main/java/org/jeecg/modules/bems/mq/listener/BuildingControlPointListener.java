package org.jeecg.modules.bems.mq.listener;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.annotation.RabbitComponent;
import org.jeecg.modules.bems.bc.service.IBuildingControlPointService;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeService;
import org.jeecg.modules.bems.mq.constant.MqConstant;
import org.jeecg.modules.bems.mq.dto.BuildingControlPointData;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

@RabbitComponent(value = "BuildingControlPointListener")
@Slf4j
@AllArgsConstructor
public class BuildingControlPointListener {

    private final IBuildingControlPointService buildingControlPointService;

    private final IDeviceAttributeService deviceAttributeService;

    @RabbitListener(queues = MqConstant.QUEUE_BUILDING_CONTROL_POINT, ackMode = "AUTO")
    public void listener(Message message){
        String body = new String(message.getBody());
        try{
            BuildingControlPointData data = JSON.parseObject(body, BuildingControlPointData.class);
            if(StringUtils.isEmpty(data.getGatewayAdr()) || StringUtils.isEmpty(data.getBacnetAdr())){
                log.error("数据错误：{}",body);
                return;
            }
            buildingControlPointService.save(data.getGatewayAdr(),data.getBacnetAdr(),data.getValue(),data.getRemark(),data.getCollectionTime());
            deviceAttributeService.updateAttributeValue(data.getGatewayAdr(),data.getBacnetAdr(),data.getValue(),data.getCollectionTime());
        }catch (Exception e){
            log.error("消息处理失败：{}", e.getMessage());
        }
    }
}
