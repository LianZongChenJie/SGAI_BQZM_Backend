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

/**
 * 告警规则
 */
@Api(tags = "告警管理-告警规则")
@RestController
@RequestMapping("/bems/alarm/rule")
@AllArgsConstructor
public class AlarmRuleController {

    private final IAlarmRulesService service;

    @ApiOperation("新增告警规则")
    @PostMapping("/add")
    @RequiresPermissions("bems:alarmRule:add")
    @AutoLog(value = "告警规则-新增")
    public Result<String> add(@RequestBody AlarmRules data){
        service.save(data);
        return Result.ok();
    }

    @ApiOperation("编辑告警规则")
    @PostMapping("/edit")
    @RequiresPermissions("bems:alarmRule:edit")
    @AutoLog(value = "告警规则-编辑")
    public Result<String> edit(@RequestBody AlarmRules data){
        service.updateById(data);
        return Result.ok();
    }

    @ApiOperation("删除告警规则")
    @DeleteMapping("/delete")
    @RequiresPermissions("bems:alarmRule:delete")
    @AutoLog(value = "告警规则-删除")
    public Result<String> delete(Long id){
        service.removeById(id);
        return Result.ok();
    }

    @ApiOperation("获取告警规则详情")
    @GetMapping("/getDetailById")
    public Result<AlarmRules> getDetailById(Long id){
        return Result.ok(service.getDetailById(id));
    }

    @ApiOperation("启用告警规则")
    @PostMapping("/startRule")
    @RequiresPermissions("bems:alarmRule:startRule")
    @AutoLog(value = "告警规则-启用")
    public Result<String> startRule(Long id){
        service.startRule(id);
        return Result.ok();
    }

    @ApiOperation("禁用告警规则")
    @PostMapping("/stopRule")
    @RequiresPermissions("bems:alarmRule:stopRule")
    @AutoLog(value = "告警规则-禁用")
    public Result<String> stopRule(Long id){
        service.stopRule(id);
        return Result.ok();
    }


    @ApiOperation("分页查询告警规则")
    @GetMapping("/listPage")
    public Result<IPage<AlarmRules>> listPage(AlarmRules params){
        return Result.ok(service.listPage(params));
    }


}
