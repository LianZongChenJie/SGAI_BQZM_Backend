package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingSceneDetailDto;
import org.jeecg.modules.bems.lighting.dto.LightingSceneDto;
import org.jeecg.modules.bems.lighting.dto.LightingSceneQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.service.ILightingSceneService;
import org.springframework.web.bind.annotation.*;

/**
 * 照明-场景管理
 * 场景独立存储于 lighting_scene 表，不绑定定时任务（lighting_plan 表），仅用于一键开关灯。
 * listPage 出参为 LightingPlan 结构，与 /bems/lighting/plan/listPage 字段一致，前端只需换 URL。
 */
@Api(tags = "照明-场景")
@RestController
@AllArgsConstructor
@RequestMapping("/bems/lighting/scene")
public class LightingSceneController {

    private final ILightingSceneService service;

    /**
     * 分页查询场景列表（出参结构同 /bems/lighting/plan/listPage）
     */
    @ApiOperation("分页查询场景列表（出参结构同 plan/listPage）")
    @GetMapping("/listPage")
    public Result<IPage<LightingPlan>> listPage(LightingSceneQueryDto params) {
        return Result.ok(service.listPage(params));
    }

    /**
     * 新增场景
     */
    @ApiOperation("新增场景（兼容 planName+relType+relIds+operationType 入参）")
    @PostMapping("/add")
    public Result<String> add(@RequestBody LightingSceneDto dto) {
        service.add(dto);
        return Result.ok();
    }

    /**
     * 编辑场景
     */
    @ApiOperation("编辑场景")
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody LightingSceneDto dto) {
        service.edit(dto);
        return Result.ok();
    }

    /**
     * 删除场景
     */
    @ApiOperation("删除场景")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        service.delete(id);
        return Result.ok();
    }

    /**
     * 场景详情（出参结构同 plan/detail：planName/relType/operationType/status/relName/areaList/circuitList）
     */
    @ApiOperation("场景详情（出参结构同 plan/detail）")
    @GetMapping("/detail")
    public Result<LightingSceneDetailDto> detail(@RequestParam Long id) {
        return Result.ok(service.getDetail(id));
    }

    /**
     * 一键执行场景
     */
    @ApiOperation("一键执行场景（按明细开/关对应区域、回路，自动记录控制日志）")
    @PostMapping("/apply")
    public Result<String> apply(@RequestParam Long id) {
        service.apply(id);
        return Result.ok();
    }
}
