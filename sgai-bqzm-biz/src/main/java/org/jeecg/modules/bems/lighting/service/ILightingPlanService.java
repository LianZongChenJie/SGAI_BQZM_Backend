package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.entity.ScheduleJob;
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

    /**
     * 从定时任务同步照明计划（创建或更新 lighting_plan + lighting_plan_execution_time）
     * 仅灯光控制类任务（区域/回路）同步，通用反射任务不处理
     * @param job 定时任务
     */
    void syncFromScheduleJob(ScheduleJob job);

    /**
     * 根据定时任务ID删除同步的照明计划（含执行时间配置）
     * @param scheduleJobId 定时任务ID
     */
    void removeByScheduleJobId(Long scheduleJobId);

    /**
     * 同步照明计划状态（启用/禁用）
     * @param scheduleJobId 定时任务ID
     * @param status 定时任务状态：1-启用 0-禁用
     */
    void syncStatusFromScheduleJob(Long scheduleJobId, Integer status);

    /**
     * 批量控制灯光（全开/全关）
     * @param relType 关联类型：区域、回路
     * @param relIds 关联ID，多个以逗号分隔
     * @param operationType 操作类型：开启、关闭（兼容 OPEN/CLOSE）
     */
    void control(String relType, String relIds, String operationType);
}
