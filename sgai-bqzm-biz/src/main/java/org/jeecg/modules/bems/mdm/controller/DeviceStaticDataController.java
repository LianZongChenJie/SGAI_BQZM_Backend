package org.jeecg.modules.bems.mdm.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.mdm.dto.DeviceStaticDataDto;
import org.jeecg.modules.bems.mdm.service.IDeviceStaticDataService;
import org.jeecg.modules.bems.vo.DeviceStaticDataVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bems/deviceStaticData")
@Api(tags="设备静态数据")
@AllArgsConstructor
public class DeviceStaticDataController {

    private final IDeviceStaticDataService service;

    @ApiOperation(value = "设备静态信息-查询", notes = "设备静态信息-查询")
    @GetMapping("/list")
    public Result<List<DeviceStaticDataVo>> list(@RequestParam(name = "type") String type, @RequestParam(name = "deviceId") Long deviceId) {
        return Result.ok(service.list(type, deviceId));
    }

    @AutoLog(value = "设备静态信息-保存")
    @ApiOperation(value = "设备静态信息-保存", notes = "设备静态信息-保存")
    @RequiresPermissions("bems:deviceStaticData:save")
    @PostMapping("/save")
    public Result<String> save(@RequestBody DeviceStaticDataDto data){
        service.save(data);
        return Result.ok();
    }
}