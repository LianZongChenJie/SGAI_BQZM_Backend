package org.jeecg.modules.bems.dataBoard.service.impl;

import lombok.AllArgsConstructor;
import org.jeecg.modules.bems.dataBoard.constant.BusinessConfigConstant;
import org.jeecg.modules.bems.dataBoard.dto.EnergyConsumptionStatisticsConfig;
import org.jeecg.modules.bems.dataBoard.service.IDataBoardService;
import org.jeecg.modules.bems.dataBoard.vo.StatisticsVo;
import org.jeecg.modules.bems.energyAnalysis.entity.*;
import org.jeecg.modules.bems.energyAnalysis.service.*;
import org.jeecg.modules.bems.energyAnalysis.vo.Chat;
import org.jeecg.modules.bems.energyAnalysis.vo.ChatSeries;
import org.jeecg.modules.bems.entity.UnitManagement;
import org.jeecg.modules.bems.service.IBusinessConfigService;
import org.jeecg.modules.bems.service.IUnitManagementService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@AllArgsConstructor
public class DataBoardServiceImpl implements IDataBoardService {

    private final IBusinessConfigService businessConfigService;
    private final IMeteringPointService meteringPointService;
    private final IUnitManagementService unitManagementService;
    private final IMeteringPointDataHourService meteringPointDataHourService;
    private final IMeteringPointDataDayService meteringPointDataDayService;
    private final IMeteringPointDataMonthService meteringPointDataMonthService;
    private final IMeteringPointDataYearService meteringPointDataYearService;

    private static final DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH");
    private static final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");
    /**
     * 获取能耗统计信息
     * @param dateType 日期类型，day,month,year
     * @return 返回能耗统计信息的对象，如果没有统计信息则返回null
     */
    @Override
    public List<StatisticsVo> getEnergyConsumptionStatistics(String dateType) {
        // 获取配置信息，格式：json
        List<EnergyConsumptionStatisticsConfig> configs = businessConfigService.getListByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_STATISTICS, EnergyConsumptionStatisticsConfig.class);
        if(CollectionUtils.isEmpty(configs)){
            return null;
        }
        List<StatisticsVo> res = new ArrayList<>();
        for (EnergyConsumptionStatisticsConfig config : configs) {
            StatisticsVo statistics = getStatisticsByPointId(dateType, config.getPointId());
            if(statistics != null){
                res.add(statistics);
            }
        }
        return res;
    }

    /**
     * 近七日电能耗趋势
     */
    @Override
    public Chat energyConsumptionPSDElectricity() {
        Long pointId = businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_ELECTRICITY);
        return energyConsumptionPSD(pointId);
    }

    /**
     * 近七日水能耗趋势
     */
    @Override
    public Chat energyConsumptionPSNWater() {
        Long pointId = businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_WATER);
        return energyConsumptionPSD(pointId);
    }

    /**
     * 能耗趋势
     * @param pointId 点位id
     * @param dateType 日期类型，day，month，year
     * @param time 日期
     * @return 返回能耗趋势的图表对象
     */
    public Chat energyConsumption(Long pointId,String name,String dateType,LocalDateTime time){
        // 获取能耗数据
        List<? extends MeteringPointData> energyData = null;
        List<Long> pointIds = new ArrayList<Long>(){{add(pointId);}};
        switch (dateType) {
            case "day":
                energyData = meteringPointDataHourService.findByTimeRangeAndPointIds(time.toLocalDate().atStartOfDay(), time, pointIds);
                break;
            case "month":
                energyData = meteringPointDataDayService.findByTimeRangeAndPointIds(time.withDayOfMonth(1).toLocalDate().atStartOfDay(),time, pointIds);
                break;
            case "year":
                energyData = meteringPointDataMonthService.findByTimeRangeAndPointIds(time.withDayOfYear(1).toLocalDate().atStartOfDay(), time, pointIds);
                break;
        }
        if(energyData == null || energyData.isEmpty()){
            return new Chat();
        }
        List<String> xAxis = new ArrayList<>();
        List<ChatSeries> series = new ArrayList<>();
        List<Object> data = new ArrayList<>();
        energyData.stream()
                .sorted(Comparator.comparing(MeteringPointData::getTime))
                .forEach(item -> {
                    switch(dateType){
                        case "day":
                            xAxis.add(item.getTime().format(hourFormatter));
                            break;
                        case "month":
                            xAxis.add(item.getTime().format(dayFormatter));
                            break;
                        case "year":
                            xAxis.add(item.getTime().format(monthFormatter));
                            break;
                    }
                    data.add(item.getValue());
                });
        series.add(new ChatSeries(name,data));
        return new Chat("",xAxis,series,null);
    }

    /**
     * 近七日能耗趋势
     * @param pointId 点位id
     */
    private Chat energyConsumptionPSD(Long pointId){
        LocalDate date = LocalDate.now();
        // 横坐标
        List<String> xAxis = IntStream.range(0, 7).mapToObj(i -> date.minusDays(7-i).format(DateTimeFormatter.ofPattern("MM-dd"))).collect(Collectors.toList());
        // 获取能耗数据
        Map<String,BigDecimal> dataMap =  meteringPointDataDayService.findByTimeRangeAndPointId(date.minusDays(7), date.minusDays(1), pointId)
                .stream()
                .filter(item -> item.getTime() != null && item.getValue() != null)
                .collect(Collectors.groupingBy(item -> item.getTime().format(DateTimeFormatter.ofPattern("MM-dd")),
                        Collectors.mapping(MeteringPointDataDay::getValue,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        Chat chat = new Chat();
        chat.setXAxis(xAxis);
        List<ChatSeries> chatSeriesList = new ArrayList<>();
        List<Object> data = new ArrayList<>();
        for(String day : xAxis){
            data.add(dataMap.getOrDefault(day, BigDecimal.ZERO));
        }
        ChatSeries chatSeries = new ChatSeries("能耗", data);
        chatSeriesList.add(chatSeries);
        chat.setChatSeriesList(chatSeriesList);
        return chat;
    }

    /**
     * 根据计量点位ID获取能耗统计信息
     */
    private StatisticsVo getStatisticsByPointId(String dateType, Long pointId) {
        StatisticsVo statistics = new StatisticsVo();
        MeteringPoint point = meteringPointService.getById(pointId);
        if(point == null){
            return null;
        }
        // 获取单位信息
        UnitManagement unit = unitManagementService.getById(point.getMeteringUnit());
        statistics.setUnit(unit == null ? "" : unit.getEnglishAme());

        LocalDateTime now = LocalDateTime.now();
        BigDecimal value = BigDecimal.ZERO;
        BigDecimal momValue = BigDecimal.ZERO;
        BigDecimal yoyValue = BigDecimal.ZERO;
        switch (dateType){
            case "day":
                MeteringPointDataDay dayValue = meteringPointDataDayService.findByDateAndPointId(now.toLocalDate(), pointId);
                value = dayValue == null ? BigDecimal.ZERO : dayValue.getValue();
                momValue = meteringPointDataHourService.findByPointIdAndTimeRange(pointId,now.toLocalDate().minusDays(1).atStartOfDay(), now.minusDays(1))
                        .stream().map(MeteringPointDataHour::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
                yoyValue = meteringPointDataHourService.findByPointIdAndTimeRange(pointId,now.toLocalDate().minusYears(1).atStartOfDay(), now.minusYears(1))
                        .stream().map(MeteringPointDataHour::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
                break;
            case "month":
                MeteringPointDataMonth monthValue = meteringPointDataMonthService.findByDateAndPointId(now.toLocalDate(), pointId);
                value = monthValue == null ? BigDecimal.ZERO : monthValue.getValue();
                momValue = meteringPointDataDayService.findByTimeRangeAndPointId(now.minusMonths(1).withDayOfMonth(1).toLocalDate(),now.minusMonths(1).toLocalDate(), pointId)
                        .stream().map(MeteringPointDataDay::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
                yoyValue = meteringPointDataDayService.findByTimeRangeAndPointId(now.minusYears(1).withDayOfMonth(1).toLocalDate(), now.minusYears(1).toLocalDate(), pointId)
                        .stream().map(MeteringPointDataDay::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
                break;
            case "year":
                MeteringPointDataYear yearValue = meteringPointDataYearService.findByDateAndPointId(now.toLocalDate(), pointId);
                value = yearValue == null ? BigDecimal.ZERO : yearValue.getValue();
                momValue = meteringPointDataMonthService.findByTimeRangeAndPointId(now.minusYears(1).withMonth(1).toLocalDate(), now.minusYears(1).toLocalDate(), pointId)
                        .stream().map(MeteringPointDataMonth::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
                yoyValue = momValue;
                break;
        }
        statistics.setName(point.getNodeName());
        statistics.setValue(value.setScale(2, RoundingMode.HALF_UP).toPlainString());
        statistics.setYoy(statistics.rate(yoyValue, value));
        statistics.setMom(statistics.rate(momValue, value));
        return statistics;
    }

}
