package org.jeecg.modules.bems.alarm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.alarm.entity.AlarmRules;
import org.jeecg.modules.bems.alarm.service.IAlarmRulesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "告警管理-告警规则")
@RestController
@RequestMapping("/bems/alarm/rules")
@AllArgsConstructor
public class AlarmRulesController {

    private final IAlarmRulesService service;

    @ApiOperation("新增告警规则")
    @PostMapping("/add")
    @RequiresPermissions("bems:alarmRules:add")
    @AutoLog(value = "告警规则-添加")
    public Result<String> add(@RequestBody AlarmRules param) {
        service.save(param);
        return Result.ok();
    }

    @ApiOperation("编辑告警规则")
    @PostMapping("/edit")
    @RequiresPermissions("bems:alarmRules:edit")
    @AutoLog(value = "告警规则-编辑")
    public Result<String> edit(@RequestBody AlarmRules param) {
        service.updateById(param);
        return Result.ok();
    }

    @ApiOperation("删除告警规则")
    @DeleteMapping("/delete")
    @RequiresPermissions("bems:alarmRules:delete")
    @AutoLog(value = "告警规则-删除")
    public Result<String> delete(@RequestParam(name = "id") Long id) {
        service.removeById(id);
        return Result.ok();
    }

    @ApiOperation("分页查询告警规则")
    @GetMapping("/listPage")
    public Result<IPage<AlarmRules>> listPage(AlarmRules params) {
        return Result.ok(service.listPage(params));
    }

    @ApiOperation("查询告警规则详情")
    @GetMapping("/detail")
    public Result<AlarmRules> detail(@RequestParam(name = "id") Long id) {
        return Result.ok(service.getDetailById(id));
    }

    @ApiOperation("获取所有告警规则")
    @GetMapping("/list")
    public Result<List<AlarmRules>> list() {
        return Result.ok(service.list());
    }

    @ApiOperation("启用告警规则")
    @PostMapping("/startRule")
    @RequiresPermissions("bems:alarmRules:startRule")
    @AutoLog(value = "告警规则-启用")
    public Result<String> startRule(@RequestParam(name = "id") Long id) {
        service.startRule(id);
        return Result.ok();
    }

    @ApiOperation("禁用告警规则")
    @PostMapping("/stopRule")
    @RequiresPermissions("bems:alarmRules:stopRule")
    @AutoLog(value = "告警规则-禁用")
    public Result<String> stopRule(@RequestParam(name = "id") Long id) {
        service.stopRule(id);
        return Result.ok();
    }
}
