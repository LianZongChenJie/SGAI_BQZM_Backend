package org.jeecg.modules.bems.patterned.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.patterned.entity.PatterningExecutionTime;
import org.jeecg.modules.bems.patterned.entity.PatterningStrategy;
import org.jeecg.modules.bems.patterned.service.IPatterningStrategyService;
import org.springframework.web.bind.annotation.*;

/**
 * 场景控制
 */
@Api(tags = "照明-场景控制")
@RestController
@RequestMapping("/bems/patterningStrategy")
@AllArgsConstructor
public class PatterningStrategyController {

    private final IPatterningStrategyService service;

    @AutoLog(value = "场景控制-添加")
    @ApiOperation("新增场景策略")
    @RequiresPermissions("bems:patterningStrategy:add")
    @PostMapping("/add")
    public Result<String> add(@RequestBody PatterningStrategy entity){
        service.save(entity);
        return Result.ok();
    }

    @AutoLog(value = "场景控制-编辑")
    @ApiOperation("编辑场景策略")
    @RequiresPermissions("bems:patterningStrategy:edit")
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody PatterningStrategy entity){
        service.updateById(entity);
        return Result.ok();
    }

    @ApiOperation("获取场景策略详情")
    @GetMapping("/getDetailById")
    public Result<PatterningStrategy> getDetailById(Long id){
        return Result.ok(service.getDetailById(id));
    }

    @AutoLog(value = "场景控制-删除")
    @ApiOperation("删除场景策略")
    @RequiresPermissions("bems:patterningStrategy:delete")
    @DeleteMapping("/delete")
    public Result<String> delete(Long id){
        service.deleteById(id);
        return Result.ok();
    }

    @AutoLog(value = "场景控制-启用")
    @ApiOperation("启用场景策略")
    @RequiresPermissions("bems:patterningStrategy:startStrategy")
    @PostMapping("/startStrategy")
    public Result<String> startStrategy(@RequestBody PatterningExecutionTime data){
        service.startStrategy(data);
        return Result.ok();
    }

    @AutoLog(value = "场景控制-禁用")
    @ApiOperation("禁用场景策略")
    @RequiresPermissions("bems:patterningStrategy:stopStrategy")
    @PostMapping("/stopStrategy")
    public Result<String> stopStrategy(@RequestParam Long id){
        service.stopStrategy(id);
        return Result.ok();
    }

    @AutoLog(value = "场景控制-立即执行")
    @ApiOperation("立即执行场景策略")
//    @RequiresPermissions("bems:patterningStrategy:executionNow")
    @PostMapping("/executionNow")
    public Result<String> executionNow(@RequestParam Long id){
        service.executeImmediately(id);
        return Result.ok();
    }

    @ApiOperation("分页查询场景策略列表")
    @GetMapping("/listPage")
    public Result<Page<PatterningStrategy>> listPage(PatterningStrategy params){
        return Result.ok(service.listPage(params));
    }
}
