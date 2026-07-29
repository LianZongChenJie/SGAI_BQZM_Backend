package org.jeecg.modules.bems.patterned.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import dm.jdbc.util.StringUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.entity.DeviceAttribute;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeService;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mq.send.MqSendService;
import org.jeecg.modules.bems.patterned.dto.DeviceAttributeOperationDto;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备属性操作服务
 */
@Slf4j
@Service
@AllArgsConstructor
public class DeviceAttributeOperationService {

    private final IDeviceService deviceService;

    private final IDeviceAttributeService deviceAttributeService;

    private final MqSendService mqSendService;

    /**
     * 设备属性操作
     * @param list deviceId,pointId,value
     */
    public void operationDeviceAttribute(Collection<DeviceAttributeOperationDto> list){
        if(CollectionUtil.isEmpty(list)){
            return;
        }
        // 获取设备信息
        Set<Long> deviceIds = list.stream().map(DeviceAttributeOperationDto::getDeviceId).collect(Collectors.toSet());
        Map<Long,Device> deviceMap = deviceService.listByIds(deviceIds)
                .stream()
                .collect(Collectors.toMap(Device::getId, Function.identity()));
        // 获取点位信息
        Set<Long> pointIds = list.stream().map(DeviceAttributeOperationDto::getPointId).collect(Collectors.toSet());
        Map<Long,DeviceAttribute> pointMap = deviceAttributeService.findByIds(pointIds)
                .stream()
                .collect(Collectors.toMap(DeviceAttribute::getId, Function.identity()));
        for (DeviceAttributeOperationDto item : list) {
            Device device = deviceMap.get(item.getDeviceId());
            DeviceAttribute deviceAttribute = pointMap.get(item.getPointId());
            if(device != null && deviceAttribute != null && StringUtil.isNotEmpty(item.getValue())){
                operationDeviceAttribute(device,deviceAttribute,item.getValue());
            }
        }
    }

    public void operationDeviceAttribute(Device device,DeviceAttribute attribute,String value){
        // TODO 设备属性操作
        // 楼控属性操作
        String acquisitionCoding = attribute.getAcquisitionCoding();
        if(StrUtil.isEmpty(acquisitionCoding)){
            return;
        }
        // 获取楼控点位信息
        String[] split = acquisitionCoding.split("-");
        if(split.length != 2){
            return;
        }
        // 发送消息
        mqSendService.sendBuildingControlOperation(split[0],split[1],value);
    }

    public void operationDeviceAttribute(Long attributeId,String value){
        DeviceAttribute attribute = deviceAttributeService.getById(attributeId);
        if(attribute == null){
            return;
        }
        String acquisitionCoding = attribute.getAcquisitionCoding();
        if(StrUtil.isEmpty(acquisitionCoding)){
            return;
        }
        // 获取楼控点位信息
        String[] split = acquisitionCoding.split("-");
        if(split.length != 2){
            return;
        }
        mqSendService.sendBuildingControlOperation(split[0],split[1],value);
    }

}
