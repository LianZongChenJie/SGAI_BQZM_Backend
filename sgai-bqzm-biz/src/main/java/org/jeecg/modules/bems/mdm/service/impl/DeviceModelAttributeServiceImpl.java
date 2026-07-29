package org.jeecg.modules.bems.mdm.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.mdm.entity.DeviceModelAttribute;
import org.jeecg.modules.bems.mdm.mapper.DeviceModelAttributeMapper;
import org.jeecg.modules.bems.mdm.service.IDeviceModelAttributeService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class DeviceModelAttributeServiceImpl extends ServiceImpl<DeviceModelAttributeMapper, DeviceModelAttribute> implements IDeviceModelAttributeService {

    @Override
    public boolean saveBatch(Collection<DeviceModelAttribute> data) {
        if(CollectionUtil.isEmpty(data)){
            return true;
        }
        return super.saveBatch(data);
    }

    @Override
    public boolean updateById(DeviceModelAttribute entity) {
        return super.updateById(entity);
    }

    @Override
    public IPage<DeviceModelAttribute> queryPage(DeviceModelAttribute params) {
        IPage<DeviceModelAttribute> page = new Page<>(params.getPageNo(),params.getPageSize());
        return page(page,new LambdaQueryWrapper<DeviceModelAttribute>()
                .eq(DeviceModelAttribute::getModelId, params.getModelId())
                .like(StringUtils.isNotEmpty(params.getAttributeName()), DeviceModelAttribute::getAttributeName, params.getAttributeName())
                .orderByAsc(DeviceModelAttribute::getSort));
    }

    @Override
    public List<DeviceModelAttribute> listByModelId(Long modelId) {
        if(modelId == null){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<DeviceModelAttribute>().eq(DeviceModelAttribute::getModelId, modelId));
    }
}
