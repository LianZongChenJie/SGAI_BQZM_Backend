package org.jeecg.modules.bems.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.mdm.entity.DeviceStaticDataConfig;
import org.jeecg.modules.bems.mdm.mapper.DeviceStaticDataConfigMapper;
import org.jeecg.modules.bems.mdm.service.IDeviceStaticDataConfigService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class DeviceStaticDataConfigServiceImpl extends ServiceImpl<DeviceStaticDataConfigMapper, DeviceStaticDataConfig> implements IDeviceStaticDataConfigService {

    @Override
    public boolean save(DeviceStaticDataConfig entity) {
        // 校验同类型下label是否重复
        if(count(new LambdaQueryWrapper<DeviceStaticDataConfig>().eq(DeviceStaticDataConfig::getType, entity.getType()).eq(DeviceStaticDataConfig::getLabel, entity.getLabel())) > 0){
            throw new JeecgBootException("同类型下label重复");
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(DeviceStaticDataConfig entity) {
        if(count(new LambdaQueryWrapper<DeviceStaticDataConfig>().eq(DeviceStaticDataConfig::getType, entity.getType()).eq(DeviceStaticDataConfig::getLabel, entity.getLabel()).ne(DeviceStaticDataConfig::getId, entity.getId())) > 0){
            throw new JeecgBootException("同类型下label重复");
        }
        return super.updateById(entity);
    }

    @Override
    public List<DeviceStaticDataConfig> list(DeviceStaticDataConfig param) {
        LambdaQueryWrapper<DeviceStaticDataConfig> wrapper = new LambdaQueryWrapper<DeviceStaticDataConfig>()
                .eq(StringUtils.isNotBlank(param.getType()),DeviceStaticDataConfig::getType,param.getType())
                .like(StringUtils.isNotBlank(param.getLabel()),DeviceStaticDataConfig::getLabel,param.getLabel())
                .orderByDesc(DeviceStaticDataConfig::getType)
                .orderByAsc(DeviceStaticDataConfig::getSort);
        return list(wrapper);
    }

    @Override
    public List<DeviceStaticDataConfig> findByType(String type) {
        if(StringUtils.isBlank(type)){
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DeviceStaticDataConfig> wrapper = new LambdaQueryWrapper<DeviceStaticDataConfig>()
                .eq(DeviceStaticDataConfig::getType,type)
                .orderByAsc(DeviceStaticDataConfig::getSort);
        return list(wrapper);
    }
}
