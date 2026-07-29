package org.jeecg.modules.bems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.entity.EnergyAttributeManagement;
import org.jeecg.modules.bems.mapper.EnergyAttributeManagementMapper;
import org.jeecg.modules.bems.service.IEnergyAttributeManagementService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 能源属性管理
 * @Author: jeecg-boot
 * @Date:   2025-02-26
 * @Version: V1.0
 */
@Service
public class EnergyAttributeManagementServiceImpl extends ServiceImpl<EnergyAttributeManagementMapper, EnergyAttributeManagement> implements IEnergyAttributeManagementService {
    @Override
    public boolean save(EnergyAttributeManagement entity) {
        if(baseMapper.selectCount(new QueryWrapper<EnergyAttributeManagement>().eq("attribute_name", entity.getAttributeName())) > 0){
            throw new JeecgBootException("属性名称重复");
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(EnergyAttributeManagement entity) {
        if(baseMapper.selectCount(new QueryWrapper<EnergyAttributeManagement>().eq("attribute_name", entity.getAttributeName()).ne("id", entity.getId())) > 0){
            throw new JeecgBootException("属性名称重复");
        }
        return super.updateById(entity);
    }
}
