package org.jeecg.modules.bems.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.mdm.dto.DeviceStaticDataDto;
import org.jeecg.modules.bems.mdm.entity.DeviceStaticData;
import org.jeecg.modules.bems.mdm.entity.DeviceStaticDataConfig;
import org.jeecg.modules.bems.mdm.mapper.DeviceStaticDataMapper;
import org.jeecg.modules.bems.mdm.service.IDeviceStaticDataConfigService;
import org.jeecg.modules.bems.mdm.service.IDeviceStaticDataService;
import org.jeecg.modules.bems.vo.DeviceStaticDataVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DeviceStaticDataServiceImpl extends ServiceImpl<DeviceStaticDataMapper, DeviceStaticData> implements IDeviceStaticDataService {

    private final IDeviceStaticDataConfigService deviceStaticDataConfigService;

    @Override
    public List<DeviceStaticDataVo> list(String type, Long deviceId) {
        List<DeviceStaticDataVo> result = new ArrayList<>();
        if(StringUtils.isBlank(type) || deviceId == null){
            return result;
        }
        // 获取type 下的所有配置项
        List<DeviceStaticDataConfig> byType = deviceStaticDataConfigService.findByType(type);
        // 获取deviceId 下的所有数据
        Map<Long,String> byDeviceId = findByDeviceId(deviceId)
                .stream().collect(Collectors.toMap(DeviceStaticData::getConfigId, DeviceStaticData::getValue));
        for(DeviceStaticDataConfig config : byType){
            result.add(new DeviceStaticDataVo(type, deviceId, config.getId(), config.getLabel(), config.getValueType(), config.getValueData(), byDeviceId.getOrDefault(config.getId(), "")));
        }
        return result;
    }

    @Override
    @Transactional
    public boolean save(DeviceStaticDataDto data) {
        if(data.getDeviceId() == null || CollectionUtils.isEmpty(data.getStaticDataList())){
            throw new JeecgBootException("参数错误！");
        }
        List<Long> configIds = data.getStaticDataList().stream().map(DeviceStaticData::getConfigId).collect(Collectors.toList());
        remove(new LambdaQueryWrapper<DeviceStaticData>().eq(DeviceStaticData::getDeviceId, data.getDeviceId()).in(DeviceStaticData::getConfigId, configIds));
        List<DeviceStaticData> collect = data.getStaticDataList().stream().peek(item -> item.setDeviceId(data.getDeviceId())).collect(Collectors.toList());
        return saveBatch(collect);
    }

    public List<DeviceStaticData> findByDeviceId(Long deviceId){
        LambdaQueryWrapper<DeviceStaticData> wrapper = new LambdaQueryWrapper<DeviceStaticData>()
                .eq(DeviceStaticData::getDeviceId,deviceId);
        return list(wrapper);
    }
}
