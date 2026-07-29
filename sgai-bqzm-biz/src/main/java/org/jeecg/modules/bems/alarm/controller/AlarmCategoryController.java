package org.jeecg.modules.bems.alarm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.alarm.entity.AlarmCategory;
import org.jeecg.modules.bems.alarm.service.IAlarmCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "告警管理-告警分类")
@RestController
@RequestMapping("/bems/alarm/category")
@AllArgsConstructor
public class AlarmCategoryController {

    private final IAlarmCategoryService service;

    @ApiOperation("新增告警分类")
    @PostMapping("/add")
    @RequiresPermissions("bems:alarmCategory:add")
    @AutoLog(value = "告警类别-添加")
    public Result<String> add(@RequestBody AlarmCategory param) {
        service.save(param);
        return Result.ok();
    }

    @ApiOperation("编辑告警分类")
    @PostMapping("/edit")
    @RequiresPermissions("bems:alarmCategory:edit")
    @AutoLog(value = "告警类别-编辑")
    public Result<String> edit(@RequestBody AlarmCategory param) {
        service.updateById(param);
        return Result.ok();
    }

    @ApiOperation("删除告警分类")
    @DeleteMapping("/delete")
    @RequiresPermissions("bems:alarmCategory:delete")
    @AutoLog(value = "告警类别-删除")
    public Result<String> delete(@RequestParam(name = "id") Long id) {
        service.removeById(id);
        return Result.ok();
    }

    @ApiOperation("启用告警分类")
    @PostMapping("/startCategory")
    @RequiresPermissions("bems:alarmCategory:startCategory")
    @AutoLog(value = "告警类别-启用")
    public Result<String> startCategory(@RequestParam(name = "id") Long id) {
        service.startCategory(id);
        return Result.ok();
    }

    @ApiOperation("停用告警分类")
    @PostMapping("/stopCategory")
    @RequiresPermissions("bems:alarmCategory:stopCategory")
    @AutoLog(value = "告警类别-停用")
    public Result<String> stopCategory(@RequestParam(name = "id") Long id) {
        service.stopCategory(id);
        return Result.ok();
    }

    @ApiOperation("分页查询告警分类")
    @GetMapping("/listPage")
    public Result<IPage<AlarmCategory>> listPage(AlarmCategory params) {
        return Result.ok(service.listPage(params));
    }

    @ApiOperation("获取所有告警分类")
    @GetMapping("/list")
    public Result<List<AlarmCategory>> list() {
        return Result.ok(service.list());
    }
}
