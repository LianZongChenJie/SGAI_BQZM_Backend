package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.service.ILightingAnalysisService;
import org.jeecg.modules.bems.lighting.vo.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 照明分析-运行时长、用电量、运行特性、故障分析
 */
@Api(tags = "照明-数据分析")
@RestController
@RequestMapping("/bems/lighting/analysis")
@AllArgsConstructor
public class LightingAnalysisController {

    private final ILightingAnalysisService analysisService;

    // ==================== 运行时长 ====================

    @ApiOperation("各地块运行时长（柱状图）")
    @GetMapping("/areaRunTime")
    public Result<List<AreaRunTimeVo>> areaRunTime(
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(analysisService.getAreaRunTime(startTime, endTime));
    }

    @ApiOperation("运行时长对比（按地块分组：地块/回路数/总运行时长/平均时长/同比）")
    @GetMapping("/runTimeCompare")
    public Result<List<AreaRunTimeCompareVo>> runTimeCompare(
            @RequestParam(required = false) List<Long> areaIds,
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(analysisService.getRunTimeCompare(areaIds, startTime, endTime));
    }

    // ==================== 用电量 ====================

    @ApiOperation("各地块用电量（折线图）")
    @GetMapping("/areaEnergy")
    public Result<List<AreaEnergyVo>> areaEnergy(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.ok(analysisService.getAreaEnergy(startDate, endDate));
    }

    @ApiOperation("用电成本分析（环图）")
    @GetMapping("/energyCost")
    public Result<List<EnergyCostVo>> energyCost(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.ok(analysisService.getEnergyCost(startDate, endDate));
    }

    @ApiOperation("用电明细-查询列表")
    @GetMapping("/energyDetail")
    public Result<IPage<EnergyDetailVo>> energyDetail(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long areaId,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.ok(analysisService.getEnergyDetail(pageNo, pageSize, areaId, startDate, endDate));
    }

    // ==================== 运行特性 ====================

    @ApiOperation("园区整体运行特性（折线图）")
    @GetMapping("/runCharacteristic")
    public Result<List<RunCharacteristicVo>> runCharacteristic(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.ok(analysisService.getRunCharacteristic(date));
    }

    @ApiOperation("夜间运行模式分析（柱状图）")
    @GetMapping("/nightRunMode")
    public Result<List<NightRunModeVo>> nightRunMode(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.ok(analysisService.getNightRunMode(startDate, endDate));
    }
}
