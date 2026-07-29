package org.jeecg.modules.bems.alarm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.alarm.entity.AlarmLevel;
import org.jeecg.modules.bems.alarm.service.IAlarmLevelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "告警管理-告警等级")
@RestController
@RequestMapping("/bems/alarm/level")
@AllArgsConstructor
public class AlarmLevelController {

    private final IAlarmLevelService service;

    @ApiOperation("新增告警等级")
    @PostMapping("/add")
    @RequiresPermissions("bems:alarmLevel:add")
    @AutoLog(value = "报警级别-添加")
    public Result<String> add(@RequestBody AlarmLevel param){
        service.save(param);
        return Result.ok();
    }

    @ApiOperation("编辑告警等级")
    @PostMapping("/edit")
    @RequiresPermissions("bems:alarmLevel:edit")
    @AutoLog(value = "报警级别-编辑")
    public Result<String> edit(@RequestBody AlarmLevel param){
        service.updateById(param);
        return Result.ok();
    }

    @ApiOperation("删除告警等级")
    @DeleteMapping("/delete")
    @RequiresPermissions("bems:alarmLevel:delete")
    @AutoLog(value = "报警级别-删除")
    public Result<String> delete(Long id){
        service.removeById(id);
        return Result.ok();
    }

    @ApiOperation("分页查询告警等级")
    @GetMapping("/listPage")
    public Result<IPage<AlarmLevel>> listPage(AlarmLevel params){
        return Result.ok(service.listPage(params));
    }

    @ApiOperation("获取所有告警等级")
    @GetMapping("/list")
    public Result<List<AlarmLevel>> list(){
        return Result.ok(service.list());
    }

    @ApiOperation("启用告警等级")
    @PostMapping("/startLevel")
    @RequiresPermissions("bems:alarmLevel:startLevel")
    @AutoLog(value = "报警级别-启用")
    public Result<String> startLevel(@RequestParam(name = "id") Long id){
        service.startLevel(id);
        return Result.ok();
    }

    @ApiOperation("禁用告警等级")
    @PostMapping("/stopLevel")
    @RequiresPermissions("bems:alarmLevel:stopLevel")
    @AutoLog(value = "报警级别-禁用")
    public Result<String> stopLevel(@RequestParam(name = "id") Long id){
        service.stopLevel(id);
        return Result.ok();
    }

}
