package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.service.ILightingEnergyStatisticsService;
import org.jeecg.modules.bems.lighting.vo.EnergyMeterReadVo;
import org.jeecg.modules.bems.lighting.vo.EnergyOverviewVo;
import org.jeecg.modules.bems.lighting.vo.EnergyProportionVo;
import org.jeecg.modules.bems.lighting.vo.EnergyRankItemVo;
import org.jeecg.modules.bems.lighting.vo.EnergySummaryItemVo;
import org.jeecg.modules.bems.lighting.vo.EnergySummaryNodeVo;
import org.jeecg.modules.bems.lighting.vo.EnergyTrendVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 照明-能耗统计（对应原型"能耗统计"页）
 * 数据源：lighting_energy_hour（整点任务聚合的小时用电量）
 */
@Api(tags = "照明-能耗统计")
@RestController
@RequestMapping("/bems/lighting/energy")
@AllArgsConstructor
public class LightingEnergyController {

    private final ILightingEnergyStatisticsService statisticsService;

    /**
     * 能耗排名（今日 kWh，降序 Top N）
     */
    @ApiOperation("能耗排名（今日 kWh，降序 Top N）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "level", value = "统计级别：parcel-按地块、zone-按区域、box-按箱子", defaultValue = "parcel", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "date", value = "日期（yyyy-MM-dd 或 yyyyMMdd），空默认今天", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "top", value = "取前 N 名", defaultValue = "15", paramType = "query", dataType = "int")
    })
    @GetMapping("/ranking")
    public Result<List<EnergyRankItemVo>> ranking(@RequestParam(defaultValue = "parcel") String level,
                                                  @RequestParam(required = false) String date,
                                                  @RequestParam(defaultValue = "15") Integer top) {
        return Result.ok(statisticsService.ranking(level, date, top));
    }

    /**
     * 占比（Top5 + 其他）
     */
    @ApiOperation("能耗占比（Top5 + 其他）")
    @GetMapping("/proportion")
    public Result<List<EnergyProportionVo>> proportion(@RequestParam(defaultValue = "parcel") String level,
                                                       @RequestParam(required = false) String date) {
        return Result.ok(statisticsService.proportion(level, date));
    }

    /**
     * 今日能耗总览（能耗排名 Top15 + 能耗占比 Top5/其他，统一按箱子）
     */
    @ApiOperation("今日能耗总览（排名 Top15 + 占比 Top5/其他）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "date", value = "日期（yyyy-MM-dd 或 yyyyMMdd），空默认今天", paramType = "query", dataType = "string")
    })
    @GetMapping("/todayOverview")
    public Result<EnergyOverviewVo> todayOverview(@RequestParam(required = false) String date) {
        return Result.ok(statisticsService.todayOverview(date));
    }

    /**
     * 逐时趋势（今日 Top5 该级别聚合对象逐时对比；level：parcel-地块、zone-区域、box-箱子）
     */
    @ApiOperation("Top5 逐时趋势对比（kW，level 支持地块/区域/箱子）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "level", value = "统计级别：parcel-按地块、zone-按区域、box-按箱子", defaultValue = "parcel", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "date", value = "日期（yyyy-MM-dd 或 yyyyMMdd），空默认今天", paramType = "query", dataType = "string")
    })
    @GetMapping("/hourlyTrend")
    public Result<EnergyTrendVo> hourlyTrend(@RequestParam(defaultValue = "parcel") String level,
                                             @RequestParam(required = false) String date) {
        return Result.ok(statisticsService.hourlyTrend(level, date));
    }

    /**
     * 汇总表（地块 → 区域 → 箱子 三层树）
     */
    @ApiOperation("能耗汇总表（地块 → 区域 → 箱子）")
    @GetMapping("/summary")
    public Result<List<EnergySummaryNodeVo>> summary(@RequestParam(required = false) String date) {
        return Result.ok(statisticsService.summary(date));
    }

    /**
     * 汇总表列表（仅网关维度，一行一个网关），支持按片区、箱子名称过滤
     */
    @ApiOperation("能耗汇总表列表（仅网关维度）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "date", value = "日期（yyyy-MM-dd 或 yyyyMMdd），空默认今天", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "districtId", value = "片区id（精确）", paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "boxName", value = "箱子名称（模糊）", paramType = "query", dataType = "string")
    })
    @GetMapping("/summaryList")
    public Result<List<EnergySummaryItemVo>> summaryList(@RequestParam(required = false) String date,
                                                         @RequestParam(required = false) Long districtId,
                                                         @RequestParam(required = false) String boxName) {
        return Result.ok(statisticsService.summaryList(date, districtId, boxName));
    }

    /**
     * 电表读数区间查询（汇总表页签：按片区/箱子/时间区间查表底与累计用电量）
     */
    @ApiOperation("电表读数区间查询（按片区/区域/箱子/时间区间）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "districtId", value = "片区ID", paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "areaCode", value = "区域编号（area_code），可精确到一个区域", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "gateway", value = "网关编号", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "startTime", value = "开始时间（yyyy-MM-dd HH:mm:ss）", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "endTime", value = "结束时间（yyyy-MM-dd HH:mm:ss）", paramType = "query", dataType = "string")
    })
    @GetMapping("/meterReads")
    public Result<List<EnergyMeterReadVo>> meterReads(@RequestParam(required = false) Long districtId,
                                                      @RequestParam(required = false) String areaCode,
                                                      @RequestParam(required = false) String gateway,
                                                      @RequestParam(required = false) String startTime,
                                                      @RequestParam(required = false) String endTime) {
        return Result.ok(statisticsService.meterReads(districtId, areaCode, gateway, startTime, endTime));
    }
}
