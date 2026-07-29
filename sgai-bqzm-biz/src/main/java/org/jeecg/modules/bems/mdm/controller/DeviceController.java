package org.jeecg.modules.bems.mdm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.bems.mdm.dto.DeviceDto;
import org.jeecg.modules.bems.mdm.dto.DeviceExportDto;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.entity.EquipmentCategory;
import org.jeecg.modules.bems.mdm.entity.Space;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mdm.service.IEquipmentCategoryService;
import org.jeecg.modules.bems.mdm.service.ISpaceService;
import org.jeecg.modules.bems.permission.annotation.DataPermission;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jeecgframework.poi.excel.ExcelExportUtil.exportExcel;

@Api(tags="设备基础信息")
@RestController
@RequestMapping("/bems/device")
@Slf4j
public class DeviceController extends JeecgController<Device, IDeviceService> {

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IEquipmentCategoryService categoryService;

    @Autowired
    private ISpaceService spaceService;

    /**
     *   添加
     *
     * @param device
     * @return
     */
    @AutoLog(value = "设备基础信息-仪表添加")
    @ApiOperation(value="设备基础信息-仪表添加", notes="设备基础信息-仪表添加")
    @RequiresPermissions("bems:device:measuring:add")
    @PostMapping(value = "/measuring/add")
    public Result<String> addMeasuring(@RequestBody Device device) {
        device.setDeviceType(Device.DEVICE_TYPE_MEASURING);
        deviceService.addDevice(device);
        return Result.OK("添加成功！");
    }

    @AutoLog(value = "设备基础信息-设备添加")
    @ApiOperation(value="设备基础信息-设备添加", notes="设备基础信息-设备添加")
    @RequiresPermissions("bems:device:equipment:add")
    @PostMapping("/equipment/add")
    public Result<String> addEquipment(@RequestBody Device device){
        device.setDeviceType(Device.DEVICE_TYPE_EQUIPMENT);
        deviceService.addDevice(device);
        return Result.OK("添加成功！");
    }

    /**
     *   编辑
     *
     * @param device
     * @return
     */
    @AutoLog(value = "设备基础信息-编辑")
    @ApiOperation(value="设备基础信息-编辑", notes="设备基础信息-编辑")
    @RequiresPermissions("bems:device:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
    public Result<String> edit(@RequestBody Device device){
        deviceService.updateById(device);
        return Result.ok("编辑成功！");
    }

    /**
     *   删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "设备基础信息-删除")
    @ApiOperation(value="设备基础信息-删除", notes="设备基础信息-删除")
    @RequiresPermissions("bems:device:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name="id",required=true) Long id){
        deviceService.removeById(id);
        return Result.OK("删除成功！");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @ApiOperation(value="设备基础信息-通过id查询", notes="设备基础信息-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<Device> queryById(@RequestParam(name="id",required=true) String id) {
        Device device = deviceService.getById(id);
        if(device==null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(device);
    }

    @ApiOperation(value="设备基础信息-仪表分页列表查询", notes="设备基础信息-仪表分页列表查询")
    @GetMapping("/measuring/list")
    @DataPermission
    public Result<IPage<Device>> listForMeasuring(DeviceDto params){
        params.setDeviceType(Device.DEVICE_TYPE_MEASURING);
        return list(params);
    }

    @ApiOperation(value="设备基础信息-设备分页列表查询", notes="设备基础信息-设备分页列表查询")
    @GetMapping("/equipment/list")
    @DataPermission
    public Result<IPage<Device>> listForEquipment(DeviceDto params){
        params.setDeviceType(Device.DEVICE_TYPE_EQUIPMENT);
        return list(params);
    }

    @ApiOperation(value="设备基础信息-分页列表查询", notes="设备基础信息-分页列表查询")
    @GetMapping("/list")
    public Result<IPage<Device>> list(DeviceDto params){
        return Result.ok(deviceService.listPage(params));
    }

    @GetMapping("/equipment/find")
    public Result<List<Device>> findForEquipment(DeviceDto params){
        params.setDeviceType(Device.DEVICE_TYPE_EQUIPMENT);
        return find(params);
    }

    @GetMapping("/measuring/find")
    public Result<List<Device>> findForMeasuring(DeviceDto params){
        params.setDeviceType(Device.DEVICE_TYPE_MEASURING);
        return find(params);
    }

    @GetMapping("/find")
    public Result<List<Device>> find(DeviceDto params){
        return Result.ok(service.list(params));
    }


    @ApiOperation(value="设备基础信息-更新自动算法", notes="设备基础信息-更新自动算法")
    @PostMapping("/updateAutomaticAlgorithm")
    @AutoLog(value = "设备基础信息-更新自动算法")
    public Result<String> updateAutomaticAlgorithm(@RequestBody Device device){
        service.updateAutomaticAlgorithm(device.getId(), device.getAutomaticAlgorithm());
        return Result.ok();
    }

    /**
     * 获取设备信息及采集信息
     */
    @GetMapping("/findDeviceAndAttribute")
    public Result<IPage<Device>> findDeviceAndAttribute(DeviceDto params){
        return Result.ok(service.findDeviceAndAttribute(params));
    }

    @GetMapping("/listDeviceAndAttribute")
    public Result<List<Device>> listDeviceAndAttribute(DeviceDto params){
        return Result.ok(service.listDeviceAndAttribute(params));
    }

    @GetMapping("/measuring/listDeviceAndAttribute")
    public Result<List<Device>> listDeviceAndAttributeForMeasuring(DeviceDto params){
        params.setDeviceType(Device.DEVICE_TYPE_MEASURING);
        return listDeviceAndAttribute(params);
    }

    @GetMapping("/equipment/listDeviceAndAttribute")
    public Result<List<Device>> listDeviceAndAttributeForEquipment(DeviceDto params){
        params.setDeviceType(Device.DEVICE_TYPE_EQUIPMENT);
        return listDeviceAndAttribute(params);
    }

    /**
     * 设备运行状态统计
     * @param categoryId 设备类别
     * @return 统计结果
     */
    @GetMapping("/deviceRunStateStatistics")
    public Result<?> deviceRunStateStatistics(@RequestParam(required = false) Long categoryId){
        return Result.ok(service.statisticsRunState(categoryId));
    }

    /**
     * 导出excel
     */
    @DataPermission
    @GetMapping("/export")
    public ModelAndView export(HttpServletRequest request,DeviceDto params){
        List<DeviceExportDto> list = service.list(params)
                .stream().map(DeviceExportDto::convert).collect(Collectors.toList());
        // 获取设备类别信息
        Map<Long,String> categoryMap = categoryService.list()
                .stream()
                .collect(Collectors.toMap(EquipmentCategory::getId, EquipmentCategory::getFullName));
        // 获取空间位置信息
        Map<Long,String> spaceMap = spaceService.list()
                .stream()
                .collect(Collectors.toMap(Space::getId, Space::getFullName));
        for(DeviceExportDto item : list){
            item.setCategoryName(categoryMap.get(item.getCategoryId()));
            item.setSpaceName(spaceMap.get(item.getSpaceId()));
        }
        LoginUser sysUser = (LoginUser)SecurityUtils.getSubject().getPrincipal();
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject("fileName", "设备信息");
        mv.addObject("entity", DeviceExportDto.class);
        ExportParams exportParams = new ExportParams( "设备信息", "导出人:" + sysUser.getRealname(), "设备信息");
        mv.addObject("params", exportParams);
        mv.addObject("data", list);
        return mv;
    }

}
