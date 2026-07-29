package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingFaultRecord;
import org.jeecg.modules.bems.lighting.mapper.LightingFaultRecordMapper;
import org.jeecg.modules.bems.lighting.service.ILightingFaultRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LightingFaultRecordServiceImpl extends ServiceImpl<LightingFaultRecordMapper, LightingFaultRecord> implements ILightingFaultRecordService {

    @Override
    public IPage<LightingFaultRecord> listPage(LightingFaultRecord params, int pageNo, int pageSize) {
        LambdaQueryWrapper<LightingFaultRecord> queryWrapper = new LambdaQueryWrapper<LightingFaultRecord>()
                .eq(StringUtils.isNotEmpty(params.getFaultType()), LightingFaultRecord::getFaultType, params.getFaultType())
                .eq(StringUtils.isNotEmpty(params.getFaultLevel()), LightingFaultRecord::getFaultLevel, params.getFaultLevel())
                .eq(StringUtils.isNotEmpty(params.getFaultStatus()), LightingFaultRecord::getFaultStatus, params.getFaultStatus())
                .like(StringUtils.isNotEmpty(params.getDeviceName()), LightingFaultRecord::getDeviceName, params.getDeviceName())
                .like(StringUtils.isNotEmpty(params.getAreaName()), LightingFaultRecord::getAreaName, params.getAreaName())
                .orderByDesc(LightingFaultRecord::getFaultTime);
        return super.page(new Page<>(pageNo, pageSize), queryWrapper);
    }

    @Override
    public List<Map<String, Object>> countByFaultType(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LightingFaultRecord> queryWrapper = new LambdaQueryWrapper<LightingFaultRecord>()
                .ge(startTime != null, LightingFaultRecord::getFaultTime, startTime)
                .le(endTime != null, LightingFaultRecord::getFaultTime, endTime);
        List<LightingFaultRecord> list = super.list(queryWrapper);
        return list.stream()
                .collect(Collectors.groupingBy(LightingFaultRecord::getFaultType, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("faultType", entry.getKey());
                    map.put("count", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> countByDate(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LightingFaultRecord> queryWrapper = new LambdaQueryWrapper<LightingFaultRecord>()
                .ge(startTime != null, LightingFaultRecord::getFaultTime, startTime)
                .le(endTime != null, LightingFaultRecord::getFaultTime, endTime)
                .orderByAsc(LightingFaultRecord::getFaultTime);
        List<LightingFaultRecord> list = super.list(queryWrapper);
        return list.stream()
                .collect(Collectors.groupingBy(
                        fault -> fault.getFaultTime().toLocalDate().toString(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("date", entry.getKey());
                    map.put("count", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
