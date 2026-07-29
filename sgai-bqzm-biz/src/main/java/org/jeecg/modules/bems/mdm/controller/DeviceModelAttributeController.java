package org.jeecg.modules.bems.mdm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.mdm.entity.DeviceModelAttribute;
import org.jeecg.modules.bems.mdm.service.IDeviceModelAttributeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备模型属性
 */
@RestController
@RequestMapping("/bems/deviceModelAttribute")
@AllArgsConstructor
@Api(tags="设备模型属性")
@Slf4j
public class DeviceModelAttributeController {

    private final IDeviceModelAttributeService service;

    @ApiOperation(value = "设备模型属性-添加", notes = "设备模型属性-添加")
    @RequiresPermissions("bems:deviceModelAttribute:add")
    @AutoLog(value = "设备模型属性-添加")
    @PostMapping("/add")
    public Result<String> add(@RequestBody List<DeviceModelAttribute> data) {
        service.saveBatch(data);
        return Result.ok("添加成功");
    }

    @ApiOperation(value = "设备模型属性-编辑", notes = "设备模型属性-编辑")
    @RequiresPermissions("bems:deviceModelAttribute:edit")
    @AutoLog(value = "设备模型属性-编辑")
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody DeviceModelAttribute data) {
        service.updateById(data);
        return Result.ok("修改成功");
    }

    @ApiOperation(value = "设备模型属性-通过id删除", notes = "设备模型属性-通过id删除")
    @RequiresPermissions("bems:deviceModelAttribute:delete")
    @AutoLog(value = "设备模型属性-通过id删除")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id")Long id){
        service.removeById(id);
        return  Result.ok();
    }

    @ApiOperation(value = "设备模型属性-分页列表查询", notes = "设备模型属性-分页列表查询")
    @GetMapping("/queryPage")
    public Result<IPage<DeviceModelAttribute>> queryPage(DeviceModelAttribute params){
        return Result.ok(service.queryPage(params));
    }

}
