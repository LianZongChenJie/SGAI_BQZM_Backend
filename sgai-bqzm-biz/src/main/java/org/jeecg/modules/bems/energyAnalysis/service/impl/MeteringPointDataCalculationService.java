package org.jeecg.modules.bems.energyAnalysis.service.impl;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointData;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataHour;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointService;
import org.jeecg.modules.bems.energyAnalysis.util.Jexl3Util;
import org.jeecg.modules.bems.entity.MeterData;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MeteringPointDataCalculationService {

    private final IDeviceService deviceService;

    private final IMeteringPointService meteringPointService;

    public List<MeteringPointData> calculation(List<Long> pointIds, List<Long> deviceIds, List<? extends MeterData> dataList){
        List<MeteringPoint> configs = meteringPointService.getByIds(pointIds);
        // 查询设备信息
        Map<Long,String> deviceCodeMap = deviceService.findByDeviceIds(deviceIds)
                .stream().collect(Collectors.toMap(Device::getId, Device::getDeviceCode));
        Map<LocalDateTime,List<MeterData>> dataMap = dataList.stream().collect(Collectors.groupingBy(MeterData::getTime,Collectors.toList()));
        // 获取dataList中所有时间
        List<LocalDateTime> dateTimes = dataList.stream().map(MeterData::getTime).distinct().toList();
        List<MeteringPointData> result = new ArrayList<>();
        for(LocalDateTime dateTime : dateTimes){
            // 获取这个小时数据
            Map<String, BigDecimal> data = dataMap.getOrDefault(dateTime, Collections.emptyList())
                    .stream()
                    .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null && item.getValue() != null)
                    .collect(Collectors.toMap(item -> deviceCodeMap.getOrDefault(item.getDeviceId(),""),MeterData::getValue));
            for(MeteringPoint point : configs){
                if(StringUtils.isEmpty(point.getTrueFormula())){
                    continue;
                }
                BigDecimal value = Jexl3Util.getValue(point.getTrueFormula(), data);
                MeteringPointDataHour item = new MeteringPointDataHour();
                item.setMeteringPointId(point.getId());
                item.setTime(dateTime);
                item.setValue(value);
                result.add(item);
            }
        }
        return result;
    }

}
