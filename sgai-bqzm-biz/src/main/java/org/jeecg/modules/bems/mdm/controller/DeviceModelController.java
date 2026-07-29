package org.jeecg.modules.bems.mdm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.mdm.entity.DeviceModel;
import org.jeecg.modules.bems.mdm.service.IDeviceModelService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备模型
 */
@RestController
@RequestMapping("/bems/deviceModel")
@AllArgsConstructor
@Api(tags="设备模型")
@Slf4j
public class DeviceModelController {

    private final IDeviceModelService service;

    @AutoLog(value = "设备模型-添加")
    @ApiOperation(value = "设备模型-添加", notes = "设备模型-添加")
    @RequiresPermissions("bems:deviceModel:add")
    @PostMapping("/add")
    public Result<String> add(@RequestBody DeviceModel data){
        service.save(data);
        return Result.OK("添加成功！");
    }

    @AutoLog(value = "设备模型-编辑")
    @ApiOperation(value = "设备模型-编辑", notes = "设备模型-编辑")
    @RequiresPermissions("bems:deviceModel:edit")
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody DeviceModel data){
        service.updateById(data);
        return Result.OK();
    }

    @AutoLog(value = "设备模型-删除")
    @ApiOperation(value = "设备模型-删除", notes = "设备模型-删除")
    @RequiresPermissions("bems:deviceModel:delete")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id")Long id){
        service.removeById(id);
        return  Result.ok();
    }

    @AutoLog(value = "设备模型-批量删除")
    @ApiOperation(value = "设备模型-批量删除", notes = "设备模型-批量删除")
    @RequiresPermissions("bems:deviceModel:deleteBatch")
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids")String ids){
        Set<Long> idList = Arrays.stream(ids.split(","))// 转换为List<Long>
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        service.removeByIds(idList);
        return Result.ok();
    }

    @ApiOperation(value = "设备模型-分页列表查询", notes = "设备模型-分页列表查询")
    @GetMapping("/queryPage")
    public Result<IPage<DeviceModel>> queryPage(DeviceModel params){
        return Result.ok(service.queryPage(params));
    }

    @ApiOperation(value = "设备模型-根据类别id查询", notes = "设备模型-根据类别id查询")
    @GetMapping("/queryByCategoryId")
    public Result<List<DeviceModel>> queryByCategoryId(@RequestParam(name = "categoryId") Long categoryId){
        return Result.ok(service.queryByCategoryId(categoryId));
    }

}
