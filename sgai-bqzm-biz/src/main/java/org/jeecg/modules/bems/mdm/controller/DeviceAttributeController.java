package org.jeecg.modules.bems.mdm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.mdm.dto.AttributeBindingDto;
import org.jeecg.modules.bems.mdm.dto.DeviceAttributeControlDto;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.entity.DeviceAttribute;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeService;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mdm.vo.DeviceAttributeDataVo;
import org.jeecg.modules.bems.patterned.service.DeviceAttributeOperationService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 设备属性信息
 */
@Api(tags = "设备属性")
@RestController
@RequestMapping("/bems/deviceAttribute")
@AllArgsConstructor
@Slf4j
public class DeviceAttributeController {
    private final IDeviceAttributeService service;

    private final IDeviceService deviceService;

    private final DeviceAttributeOperationService deviceAttributeOperationService;

    @ApiOperation(value = "添加", notes = "添加")
    @AutoLog(value = "设备属性-新增")
    @RequiresPermissions("bems:deviceAttribute:add")
    @PostMapping("/add")
    public Result<String> add(@RequestBody DeviceAttribute params){
        service.save(params);
        return Result.OK("添加成功！");
    }

    @ApiOperation(value = "编辑", notes = "编辑")
    @AutoLog(value = "设备属性-编辑")
    @RequiresPermissions("bems:deviceAttribute:edit")
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody DeviceAttribute params){
        service.updateById(params);
        return Result.ok();
    }

    @ApiOperation(value = "删除", notes = "删除")
    @AutoLog(value = "设备属性-删除")
    @RequiresPermissions("bems:deviceAttribute:delete")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id")Long id){
        service.removeById(id);
        return  Result.ok();
    }

    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/queryPage")
    public Result<IPage<DeviceAttribute>> queryPage(DeviceAttribute params){
        return Result.ok(service.queryPage(params));
    }

    /**
     * 仪表台账详情页
     * @param deviceId 设备id
     * @return 设备属性列表
     */
    @ApiOperation(value = "根据设备id查询", notes = "根据设备id查询")
    @GetMapping("/listByDeviceId")
    public Result<List<DeviceAttributeDataVo>> listByDeviceId(@RequestParam(name = "deviceId") Long deviceId){
        return Result.ok(service.listByDeviceId(deviceId));
    }

    /**
     * 查询设备关联采集点位信息
     * @param deviceId 设备id
     * @return 设备采集点位列表
     */
    @GetMapping("/getByDeviceId")
    public Result<List<DeviceAttribute>> getByDeviceId(@RequestParam(name = "deviceId")Long deviceId){
        return Result.ok(service.getByDeviceId(deviceId));
    }

    /**
     * 属性绑定点位
     */
    @PostMapping("/bindingBuildingControlPoint")
    public Result<String> bindingBuildingControlPoint(@RequestBody AttributeBindingDto data){
        service.bindingBuildingControlPoint(data);
        return Result.ok();
    }

    /**
     * 根据设备编号查询属性
     * @param deviceCode 设备编号
     * @return 设备属性列表
     */
    @GetMapping("/listByDeviceCode")
    public Result<List<DeviceAttributeDataVo>> listByDeviceCode(@RequestParam(name = "deviceCode") String deviceCode){
        Device device = deviceService.getByDeviceCode(deviceCode);
        if (device == null){
            return Result.ok(Collections.emptyList());
        }
        return Result.ok(service.listByDeviceId(device.getId()));
    }

    /**
     * 属性控制
     */
    @PostMapping("/control")
    public Result<String> control(@RequestBody DeviceAttributeControlDto param){
        deviceAttributeOperationService.operationDeviceAttribute(param.getDeviceAttributeId(),param.getValue());
        return Result.ok();
    }

    /**
     * 批量属性控制
     */
    @PostMapping("/batchControl")
    public Result<String> batchControl(@RequestBody List<DeviceAttributeControlDto> params){
        for(DeviceAttributeControlDto param : params){
            deviceAttributeOperationService.operationDeviceAttribute(param.getDeviceAttributeId(),param.getValue());
        }
        return Result.ok();
    }
}
