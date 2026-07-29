package org.jeecg.modules.bems.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.mdm.entity.DeviceModel;
import org.jeecg.modules.bems.mdm.mapper.DeviceModelMapper;
import org.jeecg.modules.bems.mdm.service.IDeviceModelService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceModelServiceImpl extends ServiceImpl<DeviceModelMapper, DeviceModel> implements IDeviceModelService {

    @Override
    public boolean save(DeviceModel entity) {
        // 校验名称是否存在
        if(baseMapper.selectCount(new LambdaQueryWrapper<DeviceModel>().eq(DeviceModel::getModelName, entity.getModelName()) ) > 0){
            throw new JeecgBootException("模型名称已存在");
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(DeviceModel entity) {
        // 校验名称是否存在
        if(baseMapper.selectCount(new LambdaQueryWrapper<DeviceModel>().eq(DeviceModel::getModelName, entity.getModelName()).ne(DeviceModel::getId, entity.getId())) > 0){
            throw new JeecgBootException("模型名称已存在");
        }
        return super.updateById(entity);
    }

    @Override
    public IPage<DeviceModel> queryPage(DeviceModel params) {
        IPage<DeviceModel> page = new Page<DeviceModel>(params.getPageNo(),params.getPageSize());
        return page(page, new LambdaQueryWrapper<DeviceModel>()
                .like(StringUtils.isNotEmpty(params.getModelName()), DeviceModel::getModelName, params.getModelName())
                .eq(params.getCategoryId() != null, DeviceModel::getCategoryId, params.getCategoryId())
                .orderByDesc(DeviceModel::getCreateTime));
    }

    @Override
    public List<DeviceModel> queryByCategoryId(Long categoryId) {
        return list(new LambdaQueryWrapper<DeviceModel>().eq(DeviceModel::getCategoryId, categoryId));
    }
}
