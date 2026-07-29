package org.jeecg.modules.bems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.entity.UnitManagement;
import org.jeecg.modules.bems.mapper.UnitManagementMapper;
import org.jeecg.modules.bems.service.IUnitManagementService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

/**
 * @Description: 计量单位管理
 * @Author: jeecg-boot
 * @Date: 2025-02-25
 * @Version: V1.0
 */
@Service
public class UnitManagementServiceImpl extends ServiceImpl<UnitManagementMapper, UnitManagement> implements IUnitManagementService {

    @Override
    public boolean save(UnitManagement entity) {
        // 校验单位代码是否重复
        if(baseMapper.selectCount(new QueryWrapper<UnitManagement>().eq("code", entity.getCode())) > 0){
            throw new JeecgBootException("单位编码重复");
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(UnitManagement entity) {
        // 校验单位代码是否重复
        if(baseMapper.selectCount(new QueryWrapper<UnitManagement>().eq("code", entity.getCode()).ne("id", entity.getId())) > 0){
            throw new JeecgBootException("单位编码重复");
        }
        return super.updateById(entity);
    }

    @Override
    public List<UnitManagement> list(){
        return super.list(new LambdaQueryWrapper<UnitManagement>().orderByAsc(UnitManagement::getSort));
    }
}
