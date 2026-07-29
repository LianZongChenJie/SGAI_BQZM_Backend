package org.jeecg.modules.bems.energyAnalysis.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.energyAnalysis.constant.BusinessConfigConstant;
import org.jeecg.modules.bems.energyAnalysis.entity.*;
import org.jeecg.modules.bems.energyAnalysis.service.*;
import org.jeecg.modules.bems.energyAnalysis.vo.*;
import org.jeecg.modules.bems.energyAnalysis.vo.chat.PieChat;
import org.jeecg.modules.bems.energyAnalysis.vo.chat.PieChatSeriesData;
import org.jeecg.modules.bems.entity.UnitManagement;
import org.jeecg.modules.bems.service.IBusinessConfigService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 碳排放分析
 */
@Service
@AllArgsConstructor
public class CarbonEmissionServiceImpl implements ICarbonEmissionService {

    private final IMeteringPointService meteringPointService;

    private final IMeteringPointDataHourService meteringPointDataHourService;

    private final IMeteringPointDataMonthService meteringPointDataMonthService;

    private final IMeteringPointDataDayService meteringPointDataDayService;

    private final IMeteringPointDataYearService meteringPointDataYearService;

    private final ICarbonEmissionFactorService carbonEmissionFactorService;

    private final IBusinessConfigService businessConfigService;

    @Override
    public List<EnergyFlowDiagramVo> getCarbonFlowChart(LocalDate date) {
        // 获取类别为电的专业拓扑的计量规则
        List<MeteringPoint> points = meteringPointService.getTreeListById(businessConfigService.getLongByKey(BusinessConfigConstant.CARBON_FLOW_DIAGRAM_KEY));
        // 获取碳排系数
        BigDecimal coefficient = carbonEmissionFactorService.getElectricityCarbonEmissionFactor();
        // 查询当月用量
        List<MeteringPointDataMonth> dataMonthList = meteringPointDataMonthService.findByDateAndPointIds(date, points.stream().map(MeteringPoint::getId).collect(Collectors.toList()))
                .stream()
                .peek(item -> {
                    if(item.getValue() == null){
                        item.setValue(BigDecimal.ZERO);
                    }else{
                        item.setValue(item.getValue().multiply(coefficient));
                    }
                })
                .collect(Collectors.toList());
        return convert(points,dataMonthList);
    }

    /**
     * 总览
     */
    @Override
    public CarbonEmissionOverviewVo getOverview(LocalDate date) {
        // 获取计量规则点位
        List<Long> pointIds = businessConfigService.getListByKey(BusinessConfigConstant.CARBON_EMISSION_POINT,Long.class);
        // 获取碳排系数
        BigDecimal coefficient = carbonEmissionFactorService.getElectricityCarbonEmissionFactor();

        // 获取当月能耗
        BigDecimal monthConsumption = meteringPointDataMonthService.findByDateAndPointIds(date, pointIds)
                .stream()
                .map(MeteringPointDataMonth::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 当月碳排
        BigDecimal monthCarbonEmission = monthConsumption.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
        // 获取上月同时期能耗
        BigDecimal lastMonthConsumption = meteringPointDataDayService.findByTimeRangeAndPointIds(date.minusMonths(1).withDayOfMonth(1).atStartOfDay(), date.minusMonths(1).atStartOfDay(), pointIds)
                .stream()
                .map(MeteringPointDataDay::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String monthConsumptionCompare = rate(monthConsumption, lastMonthConsumption);
        LocalDate startDateTheQuarter = getStartDateTheQuarter(date);
        // 本季度用能
        BigDecimal quarterConsumption = meteringPointDataMonthService.findByTimeRangeAndPointIds(startDateTheQuarter.atStartOfDay(), date.atStartOfDay(), pointIds)
                .stream()
                .map(MeteringPointDataMonth::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 本季度碳排
        BigDecimal quarterCarbonEmission = quarterConsumption.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
        // 获取上季度同时期用能
        BigDecimal lastQuarterConsumption = meteringPointDataDayService.findByTimeRangeAndPointIds(startDateTheQuarter.minusMonths(3).atStartOfDay(), startDateTheQuarter.minusMonths(3).atStartOfDay(), pointIds)
                .stream()
                .map(MeteringPointDataDay::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String quarterConsumptionCompare = rate(quarterConsumption, lastQuarterConsumption);
        // 本年用能
        BigDecimal yearConsumption = meteringPointDataYearService.findByDateAndPointIds(date, pointIds)
                .stream()
                .map(MeteringPointDataYear::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 本年碳排
        BigDecimal yearCarbonEmission = yearConsumption.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
        // 获取上一年同时期用能
        BigDecimal lastYearConsumption = meteringPointDataDayService.findByTimeRangeAndPointIds(date.minusYears(1).withMonth(1).withDayOfMonth(1).atStartOfDay(), date.minusYears(1).atStartOfDay(), pointIds)
                .stream()
                .map(MeteringPointDataDay::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String yearConsumptionCompare = rate(yearConsumption, lastYearConsumption);
        return new CarbonEmissionOverviewVo(monthConsumption, monthConsumptionCompare, quarterConsumption, quarterConsumptionCompare, yearConsumption, yearConsumptionCompare, monthCarbonEmission, monthConsumptionCompare, quarterCarbonEmission, quarterConsumptionCompare, yearCarbonEmission, yearConsumptionCompare);
    }

    @Override
    public Chat getTrendComparison(String type, LocalDate date, LocalDate compareDate) {
        // 日、月、年
        // 获取点位信息
        List<Long> pointIds = businessConfigService.getListByKey(BusinessConfigConstant.CARBON_EMISSION_POINT,Long.class);
        // 获取碳排系数
        BigDecimal coefficient = carbonEmissionFactorService.getElectricityCarbonEmissionFactor();
        List<LocalDateTime> xDateTime = getXDateTime(type, date);
        // 获取能耗数据
        Map<LocalDateTime, BigDecimal> dataMap = getData(type, date, pointIds)
                .stream()
                .collect(Collectors.groupingBy(MeteringPointData::getTime, Collectors.mapping(MeteringPointData::getValue, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        Map<LocalDateTime, BigDecimal> compareDataMap = getData(type, compareDate, pointIds)
                .stream()
                .collect(Collectors.groupingBy(MeteringPointData::getTime, Collectors.mapping(MeteringPointData::getValue, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        List<Object> data = new ArrayList<>();
        List<Object> compareData = new ArrayList<>();
        List<String> xAxis = new ArrayList<>();
        for (LocalDateTime x : xDateTime) {
            data.add(dataMap.getOrDefault(x, BigDecimal.ZERO).multiply(coefficient).setScale(2, RoundingMode.HALF_UP));
            compareData.add(compareDataMap.getOrDefault(x, BigDecimal.ZERO).multiply(coefficient).setScale(2, RoundingMode.HALF_UP));
            xAxis.add(getXAxisString(type, x));
        }
        List<ChatSeries> chatSeriesList = new ArrayList<>();
        chatSeriesList.add(new ChatSeries(getSeriesName(type, date), data));
        chatSeriesList.add(new ChatSeries(getSeriesName(type, compareDate), compareData));
        return new Chat("整体碳排放趋势对比", xAxis, chatSeriesList, null);
    }

    @Override
    public List<MeteringPoint> spatialList() {
        return meteringPointService.getElectricitySecondaryNodeForRun();
    }

    @Override
    public List<MeteringPoint> specialtyList() {
        return meteringPointService.getElectricitySecondaryForSpecialty();
    }

    /**
     * 碳对象碳排分析-饼图(月)
     *
     * @param date 月份信息
     */
    @Override
    public PieChat getSpatialCarbonEmissionAnalysis(LocalDate date) {
        List<MeteringPoint> points = meteringPointService.getTreeListById(businessConfigService.getLongByKey(BusinessConfigConstant.CARBON_FLOW_DIAGRAM_KEY));
        return getCarbonEmissionAnalysis("碳对象碳排分析", date, points);
    }

    /**
     * 碳对象碳排分析-柱状图
     *
     * @param type     类型。日、月、年
     * @param date     日期
     * @param pointIds 点位id
     */
    @Override
    public Chat getSpatialCarbonEmission(String type, LocalDate date, List<Long> pointIds) {
        List<MeteringPoint> points = meteringPointService.getByIds(pointIds);
        if (CollectionUtil.isEmpty(pointIds)) {
            // 获取空间拓扑设备类别为电的所有二级节点
            points = meteringPointService.getElectricitySecondaryNodeForSpace();
        }
        return getLineChat(type, date, points);
    }

    /**
     * 场景化碳排分析-饼图
     *
     * @param date
     */
    @Override
    public PieChat getSpecialtyCarbonEmissionAnalysis(LocalDate date) {
        List<MeteringPoint> points = meteringPointService.getElectricitySecondaryForSpecialty();
        return getCarbonEmissionAnalysis("场景化碳排分析", date, points);
    }

    /**
     * 场景化碳排分析-柱状图
     *
     * @param type     类型。日、月、年
     * @param date     日期
     * @param pointIds 点位id
     */
    @Override
    public Chat getSpecialtyCarbonEmission(String type, LocalDate date, List<Long> pointIds) {
        List<MeteringPoint> points = meteringPointService.getByIds(pointIds);
        if (CollectionUtil.isEmpty(pointIds)) {
            // 获取空间拓扑设备类别为电的所有二级节点
            points = meteringPointService.getElectricitySecondaryForSpecialty();
        }
        return getLineChat(type, date, points);
    }

    /**
     * 获取碳排数据-日
     */
    @Override
    public CarbonEmissionDataVo getCarbonEmissionForDay(LocalDateTime dateTime) {
        List<Long> pointIds = businessConfigService.getListByKey(BusinessConfigConstant.CARBON_EMISSION_POINT,Long.class);
        // 获取碳排系数
        BigDecimal emissionFactor = carbonEmissionFactorService.getElectricityCarbonEmissionFactor();
        // 获取当日数据
        BigDecimal value = meteringPointDataDayService.findByDateAndPointIds(dateTime.toLocalDate(), pointIds)
                .stream()
                .map(MeteringPointDataDay::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 获取昨日数据
        BigDecimal lastValue = meteringPointDataHourService.findByTimeRangeAndPointIds(dateTime.toLocalDate().minusDays(1).atStartOfDay(), dateTime.minusDays(1), pointIds)
                .stream()
                .map(MeteringPointDataHour::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarbonEmissionDataVo(value.multiply(emissionFactor).setScale(2, RoundingMode.HALF_UP), rate(value, lastValue), null);
    }

    /**
     * 获取碳排数据-周
     */
    @Override
    public CarbonEmissionDataVo getCarbonEmissionForWeek(LocalDate date) {
        List<Long> pointIds = businessConfigService.getListByKey(BusinessConfigConstant.CARBON_EMISSION_POINT,Long.class);
        // 获取碳排系数
        BigDecimal emissionFactor = carbonEmissionFactorService.getElectricityCarbonEmissionFactor();

        // 获取周数据
        BigDecimal value = meteringPointDataDayService.findByTimeRangeAndPointIds(getStartDateTheWeek(date).atStartOfDay(), date.atStartOfDay(), pointIds)
                .stream()
                .map(MeteringPointDataDay::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 获取上周数据
        BigDecimal lastValue = meteringPointDataDayService.findByTimeRangeAndPointIds(getStartDateTheWeek(date).minusDays(7).atStartOfDay(), date.minusDays(7).atStartOfDay(), pointIds)
                .stream()
                .map(MeteringPointDataDay::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarbonEmissionDataVo(value.multiply(emissionFactor).setScale(2, RoundingMode.HALF_UP), rate(value, lastValue), null);
    }

    /**
     * 获取碳排数据-月
     */
    @Override
    public CarbonEmissionDataVo getCarbonEmissionForMonth(LocalDate date) {
        List<Long> pointIds = businessConfigService.getListByKey(BusinessConfigConstant.CARBON_EMISSION_POINT,Long.class);
        // 获取碳排系数
        BigDecimal emissionFactor = carbonEmissionFactorService.getElectricityCarbonEmissionFactor();

        // 获取月数据
        BigDecimal value = meteringPointDataMonthService.findByTimeRangeAndPointIds(date.withDayOfMonth(1).atStartOfDay(), date.atStartOfDay(), pointIds)
                .stream()
                .map(MeteringPointDataMonth::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 获取上周数据
        BigDecimal lastValue = meteringPointDataMonthService.findByTimeRangeAndPointIds(date.minusMonths(1).withDayOfMonth(1).atStartOfDay(), date.minusMonths(1).atStartOfDay(), pointIds)
                .stream()
                .map(MeteringPointDataMonth::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarbonEmissionDataVo(value.multiply(emissionFactor).setScale(2, RoundingMode.HALF_UP), rate(value, lastValue), null);
    }

    /**
     * 获取碳排数据-季
     */
    @Override
    public CarbonEmissionDataVo getCarbonEmissionForQuarter(LocalDate date) {
        List<Long> pointIds = businessConfigService.getListByKey(BusinessConfigConstant.CARBON_EMISSION_POINT,Long.class);
        // 获取碳排系数
        BigDecimal emissionFactor = carbonEmissionFactorService.getElectricityCarbonEmissionFactor();

        // 获取季度数据
        BigDecimal value = meteringPointDataMonthService.findByTimeRangeAndPointIds(getStartDateTheQuarter(date).atStartOfDay(), date.atStartOfDay(), pointIds)
                .stream()
                .map(MeteringPointDataMonth::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 获取上季度数据
        BigDecimal lastValue = meteringPointDataMonthService.findByTimeRangeAndPointIds(getStartDateTheQuarter(date).minusMonths(3).atStartOfDay(), date.minusMonths(3).atStartOfDay(), pointIds)
                .stream()
                .map(MeteringPointDataMonth::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarbonEmissionDataVo(value.multiply(emissionFactor).setScale(2, RoundingMode.HALF_UP), rate(value, lastValue), null);
    }

    /**
     * 获取碳排数据-年
     */
    @Override
    public CarbonEmissionDataVo getCarbonEmissionForYear(LocalDate date) {
        List<Long> pointIds = businessConfigService.getListByKey(BusinessConfigConstant.CARBON_EMISSION_POINT,Long.class);
        // 获取碳排系数
        BigDecimal emissionFactor = carbonEmissionFactorService.getElectricityCarbonEmissionFactor();

        // 获取季度数据
        BigDecimal value = meteringPointDataYearService.findByDateAndPointIds(date, pointIds)
                .stream()
                .map(MeteringPointDataYear::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 获取上季度数据
        BigDecimal lastValue = meteringPointDataMonthService.findByTimeRangeAndPointIds(date.minusYears(1).withDayOfYear(1).atStartOfDay(), date.minusYears(1).atStartOfDay(), pointIds)
                .stream()
                .map(MeteringPointDataMonth::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarbonEmissionDataVo(value.multiply(emissionFactor).setScale(2, RoundingMode.HALF_UP), rate(value, lastValue), null);
    }

    private PieChat getCarbonEmissionAnalysis(String pieName, LocalDate date, List<MeteringPoint> points) {
        List<Long> pointIds = points.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        // 获取碳排系数
        BigDecimal emissionFactor = carbonEmissionFactorService.getElectricityCarbonEmissionFactor();
        Map<Long, BigDecimal> dataMap = meteringPointDataMonthService.findByDateAndPointIds(date, pointIds)
                .stream()
                .collect(Collectors.groupingBy(MeteringPointDataMonth::getMeteringPointId,
                        Collectors.reducing(BigDecimal.ZERO, MeteringPointDataMonth::getValue, BigDecimal::add)));
        List<PieChatSeriesData> seriesData = new ArrayList<>();
        for (MeteringPoint point : points) {
            seriesData.add(new PieChatSeriesData(point.getNodeName(), "", dataMap.getOrDefault(point.getId(), BigDecimal.ZERO).multiply(emissionFactor).setScale(0, RoundingMode.HALF_UP), "t"));
        }
        return new PieChat(pieName, seriesData);
    }


    private Chat getLineChat(String type, LocalDate date, List<MeteringPoint> points) {
        List<Long> pointIds = points.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        List<LocalDateTime> xDateTime = getXDateTime(type, date);
        // 获取能耗数据
        Map<Long, Map<LocalDateTime, BigDecimal>> dataMap = getData(type, date, pointIds)
                .stream()
                .collect(Collectors.groupingBy(MeteringPointData::getMeteringPointId,
                        Collectors.groupingBy(MeteringPointData::getTime,
                                Collectors.mapping(MeteringPointData::getValue,
                                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)))));
        List<ChatSeries> series = new ArrayList<>();
        for (MeteringPoint point : points) {
            List<Object> seriesData = new ArrayList<>();
            Map<LocalDateTime, BigDecimal> map = dataMap.get(point.getId());
            for (LocalDateTime x : xDateTime) {
                seriesData.add(map == null ? BigDecimal.ZERO : map.getOrDefault(x, BigDecimal.ZERO));
            }
            series.add(new ChatSeries(point.getNodeName(), seriesData));
        }
        List<String> xAxis = xDateTime.stream().map(i -> getXAxisString(type, i)).collect(Collectors.toList());
        ;
        return new Chat("", xAxis, series, null);
    }


    private List<? extends MeteringPointData> getData(String type, LocalDate date, List<Long> pointIds) {
        switch (type) {
            case "day":
                return meteringPointDataHourService.findByTimeRangeAndPointIds(date.atStartOfDay(), date.atTime(LocalTime.MAX), pointIds);
            case "month":
                return meteringPointDataDayService.findByTimeRangeAndPointIds(date.withDayOfMonth(1).atStartOfDay(), date.withDayOfMonth(date.lengthOfMonth()).atStartOfDay(), pointIds);
            case "year":
                return meteringPointDataMonthService.findByTimeRangeAndPointIds(date.withDayOfYear(1).atStartOfDay(), date.withDayOfYear(date.lengthOfYear()).atStartOfDay(), pointIds);
            default:
                throw new JeecgBootException("不支持的类型");
        }
    }

    private String getXAxisString(String type, LocalDateTime date) {
        switch (type) {
            case "day":
                return String.valueOf(date.getHour());
            case "month":
                return String.valueOf(date.getDayOfMonth());
            case "year":
                return String.valueOf(date.getMonthValue());
            default:
                throw new JeecgBootException("不支持的类型");
        }
    }

    private String getSeriesName(String type, LocalDate date) {
        switch (type) {
            case "day":
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            case "month":
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "year":
                return String.valueOf(date.getYear());
            default:
                throw new JeecgBootException("不支持的类型");
        }
    }

    private List<LocalDateTime> getXDateTime(String type, LocalDate date) {
        switch (type) {
            case "day":
                return IntStream.range(0, 24).mapToObj(i -> date.atTime(i, 0)).collect(Collectors.toList());
            case "month":
                return IntStream.range(1, date.lengthOfMonth() + 1).mapToObj(i -> date.withDayOfMonth(i).atStartOfDay()).collect(Collectors.toList());
            case "year":
                return IntStream.range(1, 13).mapToObj(i -> date.withMonth(i).atStartOfDay()).collect(Collectors.toList());
            default:
                throw new JeecgBootException("不支持的类型");
        }
    }


    /**
     * 获取当前季度的开始日期
     *
     * @param date 日期
     * @return 季度开始日期
     */
    private LocalDate getStartDateTheQuarter(LocalDate date) {
        // 获取date季度开始日期
        return date.with(date.getMonth().firstMonthOfQuarter()).withDayOfMonth(1);
    }

    /**
     * 获取本周开始日期
     *
     * @param date 日期
     * @return 周一的日期
     */
    private LocalDate getStartDateTheWeek(LocalDate date) {
        // 获取date周开始日期
        return date.with(DayOfWeek.MONDAY);
    }

    private String rate(BigDecimal value1, BigDecimal value2) {
        if (value2 == null || BigDecimal.ZERO.compareTo(value2) == 0) {
            return "100%";
        }
        // 当百分比为正时增加+号
        DecimalFormat decimalFormat = new DecimalFormat("+0.00%;-0.00%");
        return decimalFormat.format(value1.subtract(value2).divide(value2, 2, RoundingMode.HALF_UP));
    }

    private List<EnergyFlowDiagramVo> convert(List<MeteringPoint> configs, List<? extends MeteringPointData> dataList) {
        Map<Long, BigDecimal> dataMap = dataList
                .stream().collect(Collectors.toMap(MeteringPointData::getMeteringPointId, MeteringPointData::getValue, (v1, v2) -> v1));
        // 获取计量单位信息
        List<EnergyFlowDiagramVo> res = new ArrayList<>();
        Map<Long, String> nodeNameMap = configs.stream().collect(Collectors.toMap(MeteringPoint::getId, MeteringPoint::getNodeName));
//        configs.sort(Comparator.comparing(MeasureRule::getSort));
        // 同一个节点下的节点，按顺序排序
        List<Long> ids = new ArrayList<>();
        for (MeteringPoint config : configs) {
            List<MeteringPoint> children = configs.stream().filter(c -> c.getParentId().equals(config.getId())).sorted(Comparator.comparing(MeteringPoint::getSort)).collect(Collectors.toList());
            if (!ids.contains(config.getId())) {
                res.add(new EnergyFlowDiagramVo(config.getId(), config.getParentId(), nodeNameMap.getOrDefault(config.getParentId(), ""), config.getType(), config.getNodeName(), dataMap.getOrDefault(config.getId(), BigDecimal.ZERO),"","", StringUtils.isEmpty(config.getFormula()) ? "0" : "1"));
                ids.add(config.getId());
            }
            for (MeteringPoint child : children) {
                if (!ids.contains(child.getId())) {
                    ids.add(child.getId());
                    res.add(new EnergyFlowDiagramVo(child.getId(), child.getParentId(), nodeNameMap.getOrDefault(child.getParentId(), ""), child.getType(), child.getNodeName(), dataMap.getOrDefault(child.getId(), BigDecimal.ZERO),"","",StringUtils.isEmpty(config.getFormula()) ? "0" : "1"));
                }
            }
        }
        return res;
    }

}
