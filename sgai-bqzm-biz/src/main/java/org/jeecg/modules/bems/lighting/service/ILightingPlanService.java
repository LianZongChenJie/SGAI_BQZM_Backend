package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.dto.LightingPlanDetailDto;
import org.jeecg.modules.bems.lighting.dto.LightingPlanQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;

import javax.servlet.http.HttpServletResponse;

public interface ILightingPlanService extends IService<LightingPlan> {

    IPage<LightingPlan> listPage(LightingPlanQueryDto param);

    /**
     * 导出计划列表Excel（查询条件同 listPage，不分页）
     */
    void exportExcel(LightingPlanQueryDto param, HttpServletResponse response);

    void add(LightingPlan plan);

    void edit(LightingPlan plan);

    void delete(Long id);

    /**
     * 照明计划执行（MQ 消息消费端调用）
     * @param id 计划id
     * @param version 版本号
     * @return 是否执行成功（false 表示计划停用/版本不匹配/时间偏差超限等未实际执行）
     */
    boolean execution(Long id,String version);

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
     * 批量控制灯光（全开/全关），控制成功后同步更新关联场景的状态（开启/关闭）
     * @param relType 关联类型：区域、回路
     * @param relIds 关联ID，多个以逗号分隔
     * @param operationType 操作类型：开启、关闭（兼容 OPEN/CLOSE）
     * @param sceneId 场景ID（可选），指定后只同步该场景状态；为空时自动反查包含这些目标的场景
     */
    void control(String relType, String relIds, String operationType, Long sceneId, String programSceneIds);
}
