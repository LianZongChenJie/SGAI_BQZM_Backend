package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingProgramQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingProgram;
import org.jeecg.modules.bems.lighting.service.ILightingProgramService;
import org.springframework.web.bind.annotation.*;

/**
 * 照明-节目管理
 * 节目（泛光节目）独立存储于 lighting_program 表（从 lighting_scene 拆分）。
 * 场景（lighting_scene.program_scene_ids）引用节目 id，控制时按 groupId 发泛光节目MQ。
 */
@Api(tags = "照明-节目")
@RestController
@AllArgsConstructor
@RequestMapping("/bems/lighting/program")
public class LightingProgramController {

    private final ILightingProgramService service;

    /**
     * 分页查询节目列表
     */
    @ApiOperation("分页查询节目列表")
    @GetMapping("/listPage")
    public Result<IPage<LightingProgram>> listPage(LightingProgramQueryDto params) {
        return Result.ok(service.listPage(params));
    }

    /**
     * 获取全部节目列表（不分页，供前端下拉选择）
     */
    @ApiOperation("获取全部节目列表（不分页）")
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(service.list());
    }

    /**
     * 节目详情
     */
    @ApiOperation("节目详情")
    @GetMapping("/detail")
    public Result<?> detail(@RequestParam Long id) {
        return Result.ok(service.getById(id));
    }

    /**
     * 新增节目
     */
    @ApiOperation("新增节目")
    @PostMapping("/add")
    public Result<String> add(@RequestBody LightingProgram program) {
        service.add(program);
        return Result.ok();
    }

    /**
     * 编辑节目
     */
    @ApiOperation("编辑节目")
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody LightingProgram program) {
        service.edit(program);
        return Result.ok();
    }

    /**
     * 删除节目（被场景引用时禁止删除）
     */
    @ApiOperation("删除节目（被场景引用时禁止删除）")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        service.delete(id);
        return Result.ok();
    }

    /**
     * 节目开/关：直接控制单个节目（按 groupId 发泛光节目MQ，onOff：1开2关），自动记录控制日志
     */
    @ApiOperation("节目开/关（programId + operationType：开启/关闭 或 OPEN/CLOSE，按 groupId 发泛光节目MQ）")
    @PostMapping("/control")
    public Result<String> control(@RequestParam Long programId, @RequestParam String operationType) {
        service.control(programId, operationType);
        return Result.ok();
    }
}
