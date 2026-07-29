package org.jeecg.modules.bems.energyAnalysis.controller;

import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.energyAnalysis.dto.CarbonEmissionDto;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.service.ICarbonEmissionService;
import org.jeecg.modules.bems.energyAnalysis.vo.CarbonEmissionDataVo;
import org.jeecg.modules.bems.energyAnalysis.vo.CarbonEmissionOverviewVo;
import org.jeecg.modules.bems.energyAnalysis.vo.Chat;
import org.jeecg.modules.bems.energyAnalysis.vo.EnergyFlowDiagramVo;
import org.jeecg.modules.bems.energyAnalysis.vo.chat.PieChat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 碳排放分析
 */
@RestController
@RequestMapping("/bems/carbonEmission")
@AllArgsConstructor
public class CarbonEmissionController {

    private final ICarbonEmissionService service;

    @GetMapping("/getCarbonFlowChart")
    public Result<List<EnergyFlowDiagramVo>> getCarbonFlowChart(CarbonEmissionDto params){
        if(params.getDate() == null){
            params.setDate(LocalDate.now());
        }
        return Result.ok(service.getCarbonFlowChart(params.getDate()));
    }

    /**
     * 总览
     */
    @GetMapping("/getOverview")
    public Result<CarbonEmissionOverviewVo> getOverview(){
        return Result.ok(service.getOverview(LocalDate.now()));
    }

    /**
     * 碳排放趋势对比-日
     * @param params date，compareDate
     */
    @GetMapping("/getTrendComparisonForDay")
    public Result<Chat> getTrendComparisonForDay(CarbonEmissionDto params){
        return Result.ok(service.getTrendComparison("day",params.getDate(),params.getCompareDate()));
    }

    /**
     * 碳排放趋势对比-月
     * @param params date，compareDate
     */
    @GetMapping("/getTrendComparisonForMonth")
    public Result<Chat> getTrendComparisonForMonth(CarbonEmissionDto params){
        return Result.ok(service.getTrendComparison("month",LocalDate.of(params.getYear(),params.getMonth(),1),LocalDate.of(params.getCompareYear(),params.getCompareMonth(),1)));
    }

    /**
     * 碳排放趋势对比-年
     * @param params date，compareDate
     */
    @GetMapping("/getTrendComparisonForYear")
    public Result<Chat> getTrendComparisonForYear(CarbonEmissionDto params){
        return Result.ok(service.getTrendComparison("year",LocalDate.of(params.getYear(),1,1),LocalDate.of(params.getCompareYear(),1,1)));
    }

    /**
     * 碳对象列表
     */
    @GetMapping("/spatialList")
    public Result<List<MeteringPoint>> spatialList(){
        return Result.ok(service.spatialList());
    }

    /**
     * 场景化列表
     */
    @GetMapping("/specialtyList")
    public Result<List<MeteringPoint>> specialtyList(){
        return Result.ok(service.specialtyList());
    }

    /**
     * 碳对象碳排分析饼图
     */
    @GetMapping("/getSpatialCarbonEmissionAnalysis")
    public Result<PieChat> getSpatialCarbonEmissionAnalysis(){
        return Result.ok(service.getSpatialCarbonEmissionAnalysis(LocalDate.now()));
    }

    /**
     * 碳对象碳排分析-日
     */
    @GetMapping("/getSpatialCarbonEmissionForDay")
    public Result<Chat> getSpatialCarbonEmissionForDay(CarbonEmissionDto params){
        return Result.ok(service.getSpatialCarbonEmission("day",params.getDate(),strToLongList(params.getPointIds())));
    }

    /**
     * 碳对象碳排分析-月
     */
    @GetMapping("/getSpatialCarbonEmissionForMonth")
    public Result<Chat> getSpatialCarbonEmissionForMonth(CarbonEmissionDto params){
        return Result.ok(service.getSpatialCarbonEmission("month",LocalDate.of(params.getYear(),params.getMonth(),1),strToLongList(params.getPointIds())));
    }

    /**
     * 碳对象碳排分析-年
     */
    @GetMapping("/getSpatialCarbonEmissionForYear")
    public Result<Chat> getSpatialCarbonEmissionForYear(CarbonEmissionDto params){
        return Result.ok(service.getSpatialCarbonEmission("year",LocalDate.of(params.getYear(),1,1),strToLongList(params.getPointIds())));
    }

    /**
     * 场景化碳排分析-饼图
     */
    @GetMapping("/getSpecialtyCarbonEmissionAnalysis")
    public Result<PieChat> getSpecialtyCarbonEmissionAnalysis(){
        return Result.ok(service.getSpecialtyCarbonEmissionAnalysis(LocalDate.now()));
    }

    /**
     * 场景化碳排分析-日
     */
    @GetMapping("/getSpecialtyCarbonEmissionForDay")
    public Result<Chat> getSpecialtyCarbonEmissionForDay(CarbonEmissionDto params){
        return Result.ok(service.getSpecialtyCarbonEmission("day",params.getDate(),strToLongList(params.getPointIds())));
    }
    /**
     * 场景化碳排分析-月
     */
    @GetMapping("/getSpecialtyCarbonEmissionForMonth")
    public Result<Chat> getSpecialtyCarbonEmissionForMonth(CarbonEmissionDto params){
        return Result.ok(service.getSpecialtyCarbonEmission("month",LocalDate.of(params.getYear(),params.getMonth(),1),strToLongList(params.getPointIds())));
    }
    /**
     * 场景化碳排分析-年
     */
    @GetMapping("/getSpecialtyCarbonEmissionForYear")
    public Result<Chat> getSpecialtyCarbonEmissionForYear(CarbonEmissionDto params){
        return Result.ok(service.getSpecialtyCarbonEmission("year",LocalDate.of(params.getYear(),1,1),strToLongList(params.getPointIds())));
    }

    /**
     * 碳排放量-今日
     */
    @GetMapping("/getCarbonEmissionForDay")
    public Result<CarbonEmissionDataVo> getCarbonEmissionForDay(){
        return Result.ok(service.getCarbonEmissionForDay(LocalDateTime.now()));
    }

    /**
     * 碳排放量-本周
     */
    @GetMapping("/getCarbonEmissionForWeek")
    public Result<CarbonEmissionDataVo> getCarbonEmissionForWeek(){
        return Result.ok(service.getCarbonEmissionForWeek(LocalDate.now()));
    }

    /**
     * 碳排放量-本月
     */
    @GetMapping("/getCarbonEmissionForMonth")
    public Result<CarbonEmissionDataVo> getCarbonEmissionForMonth(){
        return Result.ok(service.getCarbonEmissionForMonth(LocalDate.now()));
    }

    /**
     * 碳排放量-本季度
     */
    @GetMapping("/getCarbonEmissionForQuarter")
    public Result<CarbonEmissionDataVo> getCarbonEmissionForQuarter(){
        return Result.ok(service.getCarbonEmissionForQuarter(LocalDate.now()));
    }

    /**
     * 碳排放量-本年
     */
    @GetMapping("/getCarbonEmissionForYear")
    public Result<CarbonEmissionDataVo> getCarbonEmissionForYear(){
        return Result.ok(service.getCarbonEmissionForYear(LocalDate.now()));
    }

    private List<Long> strToLongList(String str) {
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}
