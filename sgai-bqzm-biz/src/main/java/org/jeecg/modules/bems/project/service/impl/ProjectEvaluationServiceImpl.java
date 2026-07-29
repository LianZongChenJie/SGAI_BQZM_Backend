package org.jeecg.modules.bems.project.service.impl;

import dm.jdbc.util.StringUtil;
import lombok.AllArgsConstructor;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointCostData;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointData;
import org.jeecg.modules.bems.energyAnalysis.service.*;
import org.jeecg.modules.bems.energyAnalysis.vo.Chat;
import org.jeecg.modules.bems.energyAnalysis.vo.ChatSeries;
import org.jeecg.modules.bems.entity.UnitManagement;
import org.jeecg.modules.bems.mdm.constant.DeviceConstant;
import org.jeecg.modules.bems.mdm.service.IEquipmentCategoryService;
import org.jeecg.modules.bems.project.dto.EnergyMarketData;
import org.jeecg.modules.bems.project.dto.EvaluationReportData;
import org.jeecg.modules.bems.project.dto.EvaluationReportQueryDto;
import org.jeecg.modules.bems.project.entity.Project;
import org.jeecg.modules.bems.project.service.IProjectEvaluationService;
import org.jeecg.modules.bems.project.service.IProjectService;
import org.jeecg.modules.bems.project.vo.ProjectOverviewVo;
import org.jeecg.modules.bems.service.IBusinessConfigService;
import org.jeecg.modules.bems.service.IUnitManagementService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Service
@AllArgsConstructor
public class ProjectEvaluationServiceImpl implements IProjectEvaluationService {
    private final IProjectService projectService;

    private final IEquipmentCategoryService equipmentCategoryService;

    private final IMeteringPointService meteringPointService;

    private final IMeteringPointDataHourService dayDataService;
    private final IMeteringPointDataHourService hourDataService;
    private final IMeteringPointDataHourService monthDataService;
    private final IMeteringPointDataHourService yearDataService;

    private final IMeteringPointCostDataHourService costHourDataService;
    private final IMeteringPointCostDataDayService costDayDataService;
    private final IMeteringPointCostDataMonthService costMonthDataService;
    private final IMeteringPointCostDataYearService costYearDataService;

    private final ICarbonEmissionFactorService carbonEmissionFactorService;

    private final IUnitManagementService unitManagementService;

    private final DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    private final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");


    /**
     * 项目概览
     */
    @Override
    public ProjectOverviewVo getOverview() {
        List<Project> list = projectService.list();
        // 统计项目投资总额
        BigDecimal reduce = list.stream().map(Project::getProjectBudget).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProjectOverviewVo(reduce, BigDecimal.ZERO, "0.00");
    }

    /**
     * 项目投资前5排名
     *
     * @param top 默认5
     */
    @Override
    public List<Project> getInvestmentRanking(Integer top) {
        List<Project> list = projectService.list();
        if (top == null) {
            top = 5;
        }
        list.sort(Comparator.comparing(Project::getProjectBudget).reversed());
        return top.compareTo(list.size()) >= 0 ? list : list.subList(0, top);
    }

    /**
     * 获取项目评价报告
     *
     * @param param
     * @return  累计节约能耗、累计节约成本、累计减碳（电），能耗曲线图（评估曲线、基准曲线），能耗对标数据（评估时间、能耗、对标时间、对标能耗、节能量）
     */
    @Override
    public EvaluationReportData getReport(EvaluationReportQueryDto param) {
        // 获取点位信息
        MeteringPoint point = meteringPointService.getById(param.getPointId());
        if(point == null){
            throw new JeecgBootException("计量点位不存在！");
        }
        EvaluationReportData result = new EvaluationReportData();
        // 获取数据
        List<Long> pointIds = new ArrayList<Long>(){{add(point.getId());}};
        Map<String,BigDecimal> pointData = meteringPointDataToMapByDateType(findMeteringPointData(pointIds, param.getDateType(), param.getStartTime(), param.getEndTime()),param.getDateType());

        Map<String,BigDecimal> basePointData = meteringPointDataToMapByDateType(findMeteringPointData(pointIds,param.getDateType(),param.getBaseStartTime(),param.getBaseEndTime()),param.getDateType());
        // 获取横坐标信息，年：yyyy；月：yyyy-MM；日：yyyy-MM-dd
        List<String> xAxis = getXAxis(param.getDateType(),param.getStartTime(),param.getEndTime());
        List<String> baseXAxis = getXAxis(param.getDateType(), param.getBaseStartTime(), param.getBaseEndTime());
        // 实际
        List<BigDecimal> actual = new ArrayList<>();
        // 基准
        List<BigDecimal> base = new ArrayList<>();
        List<EnergyMarketData> energyMarket = new ArrayList<>();
        for(int i = 0; i < xAxis.size(); i++){
            String x = xAxis.get(i);
            String basex = i >= baseXAxis.size() ? null : baseXAxis.get(i);
            BigDecimal actualValue = pointData.get(x) == null ? BigDecimal.ZERO : pointData.get(x);
            BigDecimal baseValue = basex == null || basePointData.get(basex) == null ? BigDecimal.ZERO : basePointData.get(basex);
            actual.add(actualValue);
            base.add(baseValue == null ? BigDecimal.ZERO : baseValue);
            EnergyMarketData item = new EnergyMarketData();
            item.setEvaluationTime(x);
            item.setEnergyConsumption(actualValue.setScale(2, RoundingMode.HALF_UP).toPlainString());
            item.setBaseTime(basex);
            item.setBaseEnergyConsumption(baseValue.setScale(2, RoundingMode.HALF_UP).toPlainString());
            item.setEnergySavings(actualValue.subtract(baseValue).setScale(2, RoundingMode.HALF_UP).toPlainString());
            energyMarket.add(item);
        }
        // 能耗曲线对比
        result.setEnergyCurve(new Chat("", xAxis,
                new ArrayList<ChatSeries>(){{
                    add(new ChatSeries("评估对象", Collections.singletonList(actual)));
                    add(new ChatSeries("对标基准", Collections.singletonList(base)));
        }},null));
        // 累计节约能耗
        BigDecimal subtract = actual.stream().reduce(BigDecimal.ZERO, BigDecimal::add).subtract(base.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setEnergySavings(subtract.setScale(2, RoundingMode.HALF_UP).toPlainString());
        // 获取节约成本
        result.setSaveCosts(getSaveCosts(param).setScale(2, RoundingMode.HALF_UP).toPlainString());
        // 获取减碳量
        if(point.getCategoryId() != null && point.getCategoryId().equals(DeviceConstant.CATEGORY_ELECTRICITY)){
            result.setCarbonReductionAmount(subtract.compareTo(BigDecimal.ZERO) > 0 ? subtract.multiply(carbonEmissionFactorService.getElectricityCarbonEmissionFactor()).setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00");
        }
        // 获取能耗对标数据
        result.setEnergyMarket(energyMarket);
        // 获取能耗单位信息
        UnitManagement byId = unitManagementService.getById(point.getMeteringUnit());
        result.setEnergyUnit(byId == null ? "" : byId.getEnglishAme());
        return result;
    }


    /**
     * 获取节约成本
     */
    private BigDecimal getSaveCosts(EvaluationReportQueryDto param){
        List<Long> pointIds = new ArrayList<Long>(){{add(param.getPointId());}};
        return getCostAmount(pointIds, param.getDateType(), param.getStartTime(), param.getEndTime())
                .subtract(getCostAmount(pointIds, param.getDateType(), param.getBaseStartTime(), param.getBaseEndTime()));
    }


    private List<String> getXAxis(String dateType, LocalDateTime startTime, LocalDateTime endTime){
        dateType = StringUtil.isEmpty(dateType) ? "day" : dateType;
        switch (dateType) {
            case "hour":
                return LongStream.range(0L, ChronoUnit.HOURS.between(startTime, endTime)).mapToObj(i -> startTime.plusHours(i).format(hourFormatter)).collect(Collectors.toList());
            case "day":
                return LongStream.range(0L, ChronoUnit.DAYS.between(startTime, endTime)).mapToObj(i -> startTime.plusDays(i).format(dayFormatter)).collect(Collectors.toList());
            case "month":
                return LongStream.range(0L, ChronoUnit.MONTHS.between(startTime, endTime)).mapToObj(i -> startTime.plusMonths(i).format(monthFormatter)).collect(Collectors.toList());
            case "year":
                return LongStream.range(0L, ChronoUnit.YEARS.between(startTime, endTime)).mapToObj(i -> String.valueOf(startTime.plusYears(i).getYear())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private Map<String,BigDecimal> meteringPointDataToMapByDateType(List<? extends MeteringPointData> dataList,String dateType){
        DateTimeFormatter formatter;
        switch(dateType){
            case "hour":
                formatter = hourFormatter;
                break;
            case "day":
                formatter = dayFormatter;
                break;
            case "month":
                formatter = monthFormatter;
                break;
            case "year":
                formatter = yearFormatter;
                break;
            default:
                formatter = dayFormatter;
        }
        return dataList.stream().collect(Collectors.groupingBy(item -> item.getTime().format(formatter), Collectors.mapping(MeteringPointData::getValue, Collectors.reducing(BigDecimal.ZERO,BigDecimal::add))));
    }

    private List<? extends MeteringPointData> findMeteringPointData(List<Long> pointIds, String dateType, LocalDateTime startTime, LocalDateTime endTime) {
        dateType = StringUtil.isEmpty(dateType) ? "day" : dateType;
        switch (dateType) {
            case "hour":
                return hourDataService.findByTimeRangeAndPointIds(startTime, endTime, pointIds);
            case "day":
                return dayDataService.findByTimeRangeAndPointIds(startTime.toLocalDate().atStartOfDay(), endTime.toLocalDate().atStartOfDay(), pointIds);
            case "month":
                return monthDataService.findByTimeRangeAndPointIds(startTime.toLocalDate().atStartOfDay(), endTime.toLocalDate().atStartOfDay(), pointIds);
            case "year":
                return yearDataService.findByTimeRangeAndPointIds(startTime.toLocalDate().atStartOfDay(), endTime.toLocalDate().atStartOfDay(), pointIds);
        }
        return Collections.emptyList();
    }

    private BigDecimal getCostAmount(List<Long> pointIds,String dateType,LocalDateTime startTime,LocalDateTime endTime) {
        // 获取成本数据
        dateType = StringUtil.isEmpty(dateType) ? "day" : dateType;
        List<? extends MeteringPointCostData> costDataList = null;
        switch (dateType) {
            case "hour":
                costDataList = costHourDataService.findByTimeRangeAndPointIds(startTime, endTime, pointIds);
                break;
            case "day":
                costDataList = costDayDataService.findByTimeRangeAndPointIds(startTime.toLocalDate().atStartOfDay(), endTime.toLocalDate().atStartOfDay(), pointIds);
                break;
            case "month":
                costDataList = costMonthDataService.findByTimeRangeAndPointIds(startTime.toLocalDate().atStartOfDay(), endTime.toLocalDate().atStartOfDay(), pointIds);
                break;
            case "year":
                costDataList = costYearDataService.findByTimeRangeAndPointIds(startTime.toLocalDate().atStartOfDay(), endTime.toLocalDate().atStartOfDay(), pointIds);
                break;
        }
        if(costDataList == null || costDataList.isEmpty()){
            return BigDecimal.ZERO;
        }
        return costDataList.stream().map(MeteringPointCostData::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

//    /**
//     * 项目数量占比-各类别项目占比
//     */
//    @Override
//    public List<ProjectCategoryVo> getProportionOfProjectCategories() {
//        List<Project> list = projectService.list();
//        int all = list.size();
//        // 获取项目信息
//        Map<Long, Long> projectMap = list.stream()
//                .peek(item -> {
//                    if(item.getCategoryId() == null){
//                        item.setCategoryId(0L);
//                    }})
//                .collect(Collectors.groupingBy(Project::getCategoryId, Collectors.counting()));
//        Map<Long, String> equipmentCategories = equipmentCategoryService.queryListByType(EquipmentCategory.TYPE_MEASURING)
//                .stream()
//                .collect(Collectors.toMap(EquipmentCategory::getId, EquipmentCategory::getCategoryName));
//        List<ProjectCategoryVo> res = new ArrayList<>();
//        projectMap.forEach((key, value) -> {
//            ProjectCategoryVo projectCategoryVo = new ProjectCategoryVo();
//            projectCategoryVo.setCategoryName(equipmentCategories.getOrDefault(key, "其他"));
//            // 计算占比
//            projectCategoryVo.setValue(String.format("%.2f", (value * 100.0 / all)));
//            res.add(projectCategoryVo);
//        });
//        return res;
//    }
}
