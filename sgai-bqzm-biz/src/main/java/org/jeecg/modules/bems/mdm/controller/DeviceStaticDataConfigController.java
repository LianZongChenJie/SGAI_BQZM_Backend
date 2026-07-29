package org.jeecg.modules.bems.mdm.controller;

import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.mdm.entity.DeviceStaticDataConfig;
import org.jeecg.modules.bems.mdm.service.IDeviceStaticDataConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/bems/deviceStaticDataConfig")
public class DeviceStaticDataConfigController {

    private final IDeviceStaticDataConfigService service;

    @AutoLog(value = "设备静态信息配置-添加")
    @ApiOperation(value = "设备静态信息配置-添加", notes = "设备静态信息配置-添加")
    @RequiresPermissions("bems:deviceStaticDataConfig:add")
    @PostMapping("/add")
    public Result<String> add(@RequestBody DeviceStaticDataConfig data) {
        service.save(data);
        return Result.OK("添加成功！");
    }

    @AutoLog(value = "设备静态信息配置-删除")
    @ApiOperation(value = "设备静态信息配置-删除", notes = "设备静态信息配置-删除")
    @RequiresPermissions("bems:deviceStaticDataConfig:delete")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name="id",required=true) Long id) {
        service.removeById(id);
        return Result.ok();
    }

    @ApiOperation(value = "设备静态信息配置-查询", notes = "设备静态信息配置-查询")
    @GetMapping("/list")
    public Result<List<DeviceStaticDataConfig>> list(DeviceStaticDataConfig param) {
        return Result.ok(service.list(param));
    }

    @AutoLog(value = "设备静态信息配置-编辑")
    @ApiOperation(value = "设备静态信息配置-编辑", notes = "设备静态信息配置-编辑")
    @RequiresPermissions("bems:deviceStaticDataConfig:edit")
    @PutMapping("/edit")
    public Result<String> edit(@RequestBody DeviceStaticDataConfig data) {
        service.updateById(data);
        return Result.ok();
    }

}
