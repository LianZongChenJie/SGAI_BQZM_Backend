package org.jeecg.modules.bems.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dm.jdbc.util.StringUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.dto.DataAmendLogDto;
import org.jeecg.modules.bems.dto.DataAmendParamDto;
import org.jeecg.modules.bems.entity.DataAmendLog;
import org.jeecg.modules.bems.mapper.DataAmendLogMapper;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.service.IDataAmendLogService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DataAmendLogServiceImpl extends ServiceImpl<DataAmendLogMapper, DataAmendLog> implements IDataAmendLogService {

    private final IDeviceService deviceService;

    @Override
    public IPage<DataAmendLogDto> listPage(DataAmendParamDto param) {
        List<Device> devices = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(param.getSpaceIdList())
                || StringUtils.isNotEmpty(param.getDeviceCode())
                || StringUtil.isNotEmpty(param.getDeviceName())){
            devices = deviceService.findMeasurementBySpaceIdAndCategoryId(param.getDeviceName(),param.getDeviceCode(),param.getSpaceIdList(), null);
        }
        Page<DataAmendLog> page = new Page<>(param.getPageNo(), param.getPageSize());
        LambdaQueryWrapper<DataAmendLog> wrapper = new LambdaQueryWrapper<DataAmendLog>()
                .eq(param.getDeviceId() != null, DataAmendLog::getDeviceId, param.getDeviceId())
                .in(CollectionUtil.isNotEmpty(devices), DataAmendLog::getDeviceId, devices.stream().map(Device::getId).collect(Collectors.toSet()))
                .orderByDesc(DataAmendLog::getUpdateTime);
        if(StringUtils.isNotEmpty(param.getAmendType())){
            if("系统修正".equals(param.getAmendType())){
                wrapper.eq(DataAmendLog::getUpdateBy, "系统修正");
            }
            if("人工修正".equals(param.getAmendType())){
                wrapper.ne(DataAmendLog::getUpdateBy, "系统修正");
            }
        }
        IPage<DataAmendLogDto> result = super.page(page, wrapper).convert(DataAmendLogDto::convert);
        devices = deviceService.findByDeviceIds(result.getRecords().stream().map(DataAmendLogDto::getDeviceId).toList());
        Map<Long,Device> deviceMap = devices
                        .stream().collect(Collectors.toMap(Device::getId, Function.identity(),(k1,k2) -> k2));
        result.getRecords().forEach(log -> {
            Device device = deviceMap.get(log.getDeviceId());
            if(device != null) {
                log.setDeviceName(device.getDeviceName());
                log.setDeviceCode(device.getDeviceCode());
            }
        });
        return result;
    }
}
