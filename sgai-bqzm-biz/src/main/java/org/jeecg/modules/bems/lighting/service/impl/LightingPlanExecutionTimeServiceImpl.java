package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.mapper.LightingPlanExecutionTimeMapper;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecutionTimeService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class LightingPlanExecutionTimeServiceImpl extends ServiceImpl<LightingPlanExecutionTimeMapper,LightingPlanExecutionTime> implements ILightingPlanExecutionTimeService {
    @Override
    public LightingPlanExecutionTime getByPlanId(Long planId) {
        return super.getOne(new LambdaQueryWrapper<LightingPlanExecutionTime>()
                .eq(LightingPlanExecutionTime::getPlanId,planId));
    }

    @Override
    public boolean saveOrUpdate(LightingPlanExecutionTime entity) {
        entity.setVersion(UUID.randomUUID().toString(true));
        return super.saveOrUpdate(entity);
    }

    @Override
    public List<LightingPlanExecutionTime> getByPlanIds(List<Long> planIds) {
        if(CollectionUtil.isEmpty(planIds)){
            return Collections.emptyList();
        }
        return super.list(new LambdaQueryWrapper<LightingPlanExecutionTime>().in(LightingPlanExecutionTime::getPlanId,planIds));
    }

    @Override
    public LightingPlanExecutionTime getByPlanIdAndVersion(Long planId, String version) {
        if(planId == null || version == null){
            return null;
        }
        return super.getOne(new LambdaQueryWrapper<LightingPlanExecutionTime>()
                .eq(LightingPlanExecutionTime::getPlanId,planId)
                .eq(LightingPlanExecutionTime::getVersion,version));
    }
}
