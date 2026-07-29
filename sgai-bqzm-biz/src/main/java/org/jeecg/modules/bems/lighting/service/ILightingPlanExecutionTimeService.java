package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;

import java.util.List;

public interface ILightingPlanExecutionTimeService extends IService<LightingPlanExecutionTime> {

    LightingPlanExecutionTime getByPlanId(Long planId);

    boolean saveOrUpdate(LightingPlanExecutionTime data);

    List<LightingPlanExecutionTime> getByPlanIds(List<Long> planIds);

    LightingPlanExecutionTime getByPlanIdAndVersion(Long planId,String version);

}
