package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.dto.LightingSceneDetailDto;
import org.jeecg.modules.bems.lighting.dto.LightingSceneDto;
import org.jeecg.modules.bems.lighting.dto.LightingSceneQueryDto;
import org.jeecg.modules.bems.lighting.dto.LightingSpaceScenesVo;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingScene;

/**
 * 照明场景 Service
 * 场景独立存储于 lighting_scene 表，不绑定定时任务（lighting_plan 表），仅用于一键开关灯。
 * listPage 出参映射为 LightingPlan 结构，与 /bems/lighting/plan/listPage 字段一致，前端只需换 URL。
 */
public interface ILightingSceneService extends IService<LightingScene> {

    /**
     * 分页查询场景列表（出参为 LightingPlan 结构，与 plan/listPage 一致：planName/relType/relIds/operationType/status/sort）
     */
    IPage<LightingPlan> listPage(LightingSceneQueryDto params);

    /**
     * 新增场景（含明细）
     */
    void add(LightingSceneDto dto);

    /**
     * 编辑场景（含明细，先删后插）
     */
    void edit(LightingSceneDto dto);

    /**
     * 删除场景（含明细）
     */
    void delete(Long id);

    /**
     * 场景详情（出参结构同 plan/detail：planName/relType/operationType/status/relName/areaList/circuitList 等）
     */
    LightingSceneDetailDto getDetail(Long id);

    /**
     * 一键执行场景：按明细逐个控制目标（区域/回路开或关），自动记录控制日志
     */
    void apply(Long id);

    /**
     * 按空间查询：该空间下的所有场景（含明细）和所有回路（含区域名/空间名）
     * 场景归属规则：场景明细中任一目标（区域或回路）属于该空间即视为该空间的场景
     *
     * @param spaceId 空间id（lighting_area.space）
     */
    LightingSpaceScenesVo getBySpace(String spaceId);
}
