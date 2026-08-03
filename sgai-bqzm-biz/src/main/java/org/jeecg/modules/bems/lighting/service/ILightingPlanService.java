package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.dto.LightingPlanDetailDto;
import org.jeecg.modules.bems.lighting.dto.LightingPlanQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;

public interface ILightingPlanService extends IService<LightingPlan> {

    IPage<LightingPlan> listPage(LightingPlanQueryDto param);

    void add(LightingPlan plan);

    void edit(LightingPlan plan);

    void delete(Long id);

    /**
     * 照明计划执行
     * @param id 计划id
     * @param version 版本号
     */
    void execution(Long id,String version);

    void enable(LightingPlanExecutionTime data);

    void disable(Long id);

    /**
     * 获取计划详情
     * @param id 计划id
     * @return 详情信息
     */
    LightingPlanDetailDto getDetail(Long id);

    /**
     * 现在执行
     * @param id 计划id
     */
    void executionNow(Long id);


    void removeByScheduleJobId(Long scheduleJobId);

    /**
     * 批量控制灯光（全开/全关）
     * @param relType 关联类型：区域、回路
     * @param relIds 关联ID，多个以逗号分隔
     * @param operationType 操作类型：开启、关闭（兼容 OPEN/CLOSE）
     */
    void control(String relType, String relIds, String operationType);
}
