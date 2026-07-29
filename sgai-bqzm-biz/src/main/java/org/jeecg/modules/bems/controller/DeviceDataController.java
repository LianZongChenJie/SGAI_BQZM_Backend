package org.jeecg.modules.bems.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.bems.dto.DeviceDataFindDto;
import org.jeecg.modules.bems.dto.DeviceHourDataAmendDto;
import org.jeecg.modules.bems.entity.*;
import org.jeecg.modules.bems.mdm.dto.DeviceRunStateStatisticsDto;
import org.jeecg.modules.bems.mdm.entity.EquipmentCategory;
import org.jeecg.modules.bems.mdm.entity.Space;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mdm.service.IEquipmentCategoryService;
import org.jeecg.modules.bems.mdm.service.ISpaceService;
import org.jeecg.modules.bems.permission.annotation.DataPermission;
import org.jeecg.modules.bems.service.*;
import org.jeecg.modules.bems.vo.DeviceDataVo;
import org.jeecg.modules.bems.vo.DeviceDataWaterVo;
import org.jeecg.modules.bems.vo.HourDataVo;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/bems/deviceData")
public class DeviceDataController {

    private final IDeviceDataService service;
    private final IDeviceService deviceService;
    private final IMinuteDataService minuteDataService;
    private final IHourDataService hourDataService;
    private final IDayDataService dayDataService;
    private final IMonthDataService monthDataService;
    private final IYearDataService yearDataService;
    private final IRealDataService realDataService;

    private final IEquipmentCategoryService equipmentCategoryService;
    private final ISpaceService spaceService;

    @DataPermission
    @GetMapping("/list")
    public Result<IPage<DeviceDataVo>> list(DeviceDataFindDto params) {
        return Result.ok(service.findList(params));
    }

    @DataPermission
    @GetMapping("/deviceStatusList")
    public Result<IPage<DeviceDataVo>> deviceStatusList(DeviceDataFindDto params){
        return Result.ok(service.deviceStatusList(params));
    }

    @GetMapping("/findHourData")
    public Result<List<HourData>> findHourData(DeviceDataFindDto params) {
        return Result.ok(hourDataService.findByDeviceIdAndTimeRange(params.getDeviceId(), params.getStartTime(), params.getEndTime()));
    }

    @DataPermission
    @GetMapping("/hour/listPage")
    public Result<IPage<HourDataVo>> hourListPage(DeviceDataFindDto params){
        List<Long> spaceIdList = StringUtils.isEmpty(params.getSpaceIds()) ? null : new ArrayList<>(Arrays.asList(params.getSpaceIds().split(","))).stream().map(Long::valueOf).collect(Collectors.toList());
        List<Long> categoryIdList = StringUtils.isEmpty(params.getCategoryIds()) ? null : new ArrayList<>(Arrays.asList(params.getCategoryIds().split(","))).stream().map(Long::valueOf).collect(Collectors.toList());
//        params.setSpaceIdList(new ArrayList<Long>(){{add(1L);}});
        params.setSpaceIdList(spaceIdList);
        params.setCategoryIdList(categoryIdList);
        return Result.ok(hourDataService.listPage(params));
    }

    @AutoLog(value = "计量设备数据修正")
    @RequiresPermissions("bems:device_data:amend")
    @PostMapping("/hourDataAmend")
    public Result<String> hourDataAmend(@RequestBody List<DeviceHourDataAmendDto> params){
        params.forEach(item -> service.dataAmend(item.getId(), item.getValue()));
        return Result.ok();
    }

    @GetMapping("/minute/list")
    public Result<IPage<MinuteData>> listForMinute(DeviceDataFindDto params) {
        return Result.ok(minuteDataService.page(new Page<>(params.getPageNo(),params.getPageSize()), new LambdaQueryWrapper<MinuteData>()
                .eq(MinuteData::getDeviceId, params.getDeviceId())
                .between(params.getStartTime() != null && params.getEndTime() != null, MinuteData::getTime, params.getStartTime(), params.getEndTime())
                .orderByDesc(MinuteData::getTime)));
    }

    @GetMapping("/hour/list")
    public Result<IPage<HourData>> listForHour(DeviceDataFindDto params) {
        return Result.ok(hourDataService.page(new Page<>(params.getPageNo(),params.getPageSize()), new LambdaQueryWrapper<HourData>()
                .eq(HourData::getDeviceId, params.getDeviceId())
                .between(params.getStartTime() != null && params.getEndTime() != null, HourData::getTime, params.getStartTime(), params.getEndTime())
                .orderByDesc(HourData::getTime)));
    }

    @GetMapping("/day/list")
    public Result<IPage<DayData>> listForDay(DeviceDataFindDto params) {
        return Result.ok(dayDataService.page(new Page<>(params.getPageNo(),params.getPageSize()), new LambdaQueryWrapper<DayData>()
                .eq(DayData::getDeviceId, params.getDeviceId())
                .between(params.getStartTime() != null && params.getEndTime() != null, DayData::getTime, params.getStartTime(), params.getEndTime())
                .orderByDesc(DayData::getTime)));
    }

    @GetMapping("/month/list")
    public Result<IPage<MonthData>> listForMonth(DeviceDataFindDto params) {
        return Result.ok(monthDataService.page(new Page<>(params.getPageNo(),params.getPageSize()), new LambdaQueryWrapper<MonthData>()
                .eq(MonthData::getDeviceId, params.getDeviceId())
                .between(params.getStartTime() != null && params.getEndTime() != null, MonthData::getTime, params.getStartTime(), params.getEndTime())
                .orderByDesc(MonthData::getTime)));
    }

    @GetMapping("/year/list")
    public Result<IPage<YearData>> listForYear(DeviceDataFindDto params) {
        return Result.ok(yearDataService.page(new Page<>(params.getPageNo(),params.getPageSize()), new LambdaQueryWrapper<YearData>()
                .eq(YearData::getDeviceId, params.getDeviceId())
                .between(params.getStartTime() != null && params.getEndTime() != null, YearData::getTime, params.getStartTime(), params.getEndTime())
                .orderByDesc(YearData::getTime)));
    }

    @GetMapping("/real/list")
    public Result<IPage<RealData>> listForReal(DeviceDataFindDto params){
        return Result.ok(realDataService.page(new Page<>(params.getPageNo(),params.getPageSize()),new LambdaQueryWrapper<RealData>()
                .eq(RealData::getDeviceId, params.getDeviceId())
                .between(params.getStartTime() != null && params.getEndTime() != null, RealData::getTime, params.getStartTime(), params.getEndTime())
                .orderByDesc(RealData::getTime)));
    }

    /**
     * 统计计量设备运行状态
     * @return
     */
    @GetMapping("/statisticsRunState")
    public Result<DeviceRunStateStatisticsDto> statisticsRunState(@RequestParam(required = false) Long categoryId){
        return Result.ok(deviceService.statisticsRunState());
    }

    /**
     * 状态数据导出
     */
    @DataPermission
    @GetMapping("/export")
    public ModelAndView export(HttpServletRequest request, DeviceDataFindDto params){
        params.setPageNo(1);
        params.setPageSize(9999);
        IPage<DeviceDataVo> list = service.findList(params);
        Map<Long,String> spaceMap = spaceService.list()
                .stream()
                .collect(Collectors.toMap(Space::getId, Space::getFullName));
        Map<Long,String> categoryMap = equipmentCategoryService.list()
                .stream()
                .collect(Collectors.toMap(EquipmentCategory::getId, EquipmentCategory::getFullName));
        List<DeviceDataVo> records = list.getRecords();
        for(DeviceDataVo item : records){
            item.setCategoryName(categoryMap.get(item.getCategoryId()));
            item.setSpaceName(spaceMap.get(item.getSpaceId()));
        }
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject("fileName", "设备状态数据");
        mv.addObject("entity", DeviceDataVo.class);
        ExportParams exportParams = new ExportParams( "设备状态数据", "导出人:" + sysUser.getRealname(), "设备状态数据");
        mv.addObject("params", exportParams);
        mv.addObject("data", records);
        return mv;
    }

    /**
     * 状态数据，
     * @param params
     * @return
     */
    @DataPermission
    @GetMapping("/list1")
    public Result<IPage<DeviceDataVo>> list1(DeviceDataFindDto params){
        return Result.ok(service.findList1(params));
    }

    /**
     * 水表状态数据导出
     */
    @GetMapping("/export1")
    public ModelAndView exportForWater(HttpServletRequest request, DeviceDataFindDto params){
        params.setPageNo(1);
        params.setPageSize(9999);
        IPage<DeviceDataVo> list = service.findList1(params);
        Map<Long,String> spaceMap = spaceService.list()
                .stream()
                .collect(Collectors.toMap(Space::getId, Space::getFullName));
        Map<Long,String> categoryMap = equipmentCategoryService.list()
                .stream()
                .collect(Collectors.toMap(EquipmentCategory::getId, EquipmentCategory::getFullName));
        List<DeviceDataVo> records = list.getRecords();
        List<DeviceDataWaterVo> data = new ArrayList<>();
        for(DeviceDataVo item : records){
            item.setCategoryName(categoryMap.get(item.getCategoryId()));
            item.setSpaceName(spaceMap.get(item.getSpaceId()));
            data.add(DeviceDataWaterVo.convert(item));
        }
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject("fileName", "设备状态数据");
        mv.addObject("entity", DeviceDataWaterVo.class);
        ExportParams exportParams = new ExportParams( "设备状态数据", "导出人:" + sysUser.getRealname(), "设备状态数据");
        mv.addObject("params", exportParams);
        mv.addObject("data", data);
        return mv;
    }

}
