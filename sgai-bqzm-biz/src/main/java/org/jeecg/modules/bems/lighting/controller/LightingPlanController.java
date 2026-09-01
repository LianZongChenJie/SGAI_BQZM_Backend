package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingPlanControlDto;
import org.jeecg.modules.bems.lighting.dto.LightingPlanDetailDto;
import org.jeecg.modules.bems.lighting.dto.LightingPlanQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.jeecg.modules.bems.permission.annotation.ButtonPermission;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * 照明计划
 */
@Api(tags = "照明-计划")
@RestController
@AllArgsConstructor
@RequestMapping("/bems/lighting/plan")
public class LightingPlanController {

    private final ILightingPlanService service;

    /**
     * 分页查询计划列表
     */
    @ApiOperation("分页查询计划列表")
    @GetMapping("/listPage")
    public Result<IPage<LightingPlan>> listPage(LightingPlanQueryDto params){
        return Result.ok(service.listPage(params));
    }

    /**
     * 导出计划列表Excel（查询条件同 listPage，不分页）
     */
    @ApiOperation("导出计划列表Excel")
    @GetMapping("/export")
    public void export(LightingPlanQueryDto params, HttpServletResponse response){
        service.exportExcel(params, response);
    }

    /**
     * 新增计划
     */
    @ApiOperation("新增计划")
    @ButtonPermission("northAreaLighting:switch")
    @PostMapping("/add")
    public Result<String> add(@RequestBody LightingPlan plan){
        service.add(plan);
        return Result.ok();
    }

    /**
     * 编辑计划
     */
    @ApiOperation("编辑计划")
    @ButtonPermission("northAreaLighting:switch")
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody LightingPlan plan){
        service.edit(plan);
        return Result.ok();
    }

    /**
     * 删除计划
     */
    @ApiOperation("删除计划")
    @ButtonPermission("northAreaLighting:switch")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long id){
        service.delete(id);
        return Result.ok();
    }

    /**
     * 启用计划
     */
    @ApiOperation("启用计划")
    @RequiresPermissions("northAreaLighting:switch")
    @PostMapping("/enable")
    public Result<String> enable(@RequestBody LightingPlanExecutionTime data){
        service.enable(data);
        return Result.ok();
    }

    /**
     * 停用计划
     */
    @ApiOperation("停用计划")
    @RequiresPermissions("northAreaLighting:switch")
    @PostMapping("/disable")
    public Result<String> disable(@RequestParam Long id){
        service.disable(id);
        return Result.ok();
    }

    /**
     * 计划详情
     */
    @ApiOperation("计划详情")
    @GetMapping("/detail")
    public Result<LightingPlanDetailDto> detail(@RequestParam Long id){
        return Result.ok(service.getDetail(id));
    }

    /**
     * 立即执行计划
     */
    @ApiOperation("立即执行计划")
    @RequiresPermissions("northAreaLighting:switch")
    @PostMapping("/executeNow")
    public Result<String> executeNow(@RequestParam Long id){
        service.executionNow(id);
        return Result.ok();
    }

    /**
     * 批量控制灯光（全开/全关）
     * 按计划列表信息中的类型（relType + relIds + operationType）控制目标灯，
     * 控制成功后同步更新关联场景的 status（开启/关闭）。
     * sceneId 可选：传了则只同步该场景，不传自动反查包含这些目标的场景。
     */
    @ApiOperation("批量控制灯光（全开/全关）")
    @RequiresPermissions("northAreaLighting:switch")
    @PostMapping("/control")
    public Result<String> control(@RequestBody LightingPlanControlDto dto){
        service.control(dto.getRelType(), dto.getRelIds(), dto.getOperationType(), dto.getSceneId(), dto.getProgramSceneIds());
        return Result.ok();
    }
}
