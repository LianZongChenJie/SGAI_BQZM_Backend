package org.jeecg.modules.bems.energyAnalysis.service.impl;

import lombok.AllArgsConstructor;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.energyAnalysis.entity.*;
import org.jeecg.modules.bems.energyAnalysis.service.*;
import org.jeecg.modules.bems.energyAnalysis.util.TableUtil;
import org.jeecg.modules.bems.energyAnalysis.util.pricing.CostCalculationUtil;
import org.jeecg.modules.bems.energyAnalysis.util.pricing.LadderPricing;
import org.jeecg.modules.bems.energyAnalysis.vo.*;
import org.jeecg.modules.bems.energyAnalysis.vo.chat.PieChat;
import org.jeecg.modules.bems.energyAnalysis.vo.chat.PieChatSeriesData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 成本分析
 */
@Service
@AllArgsConstructor
public class CostAnalysisServiceImpl implements ICostAnalysisService {
    private final IEnergyPricingConfigService energyPricingConfigService;

    private final IMeteringPointDataHourService meteringPointDataHourService;

    private final IMeteringPointDataDayService meteringPointDataDayService;

    private final IMeteringPointDataMonthService meteringPointDataMonthService;

    private final IMeteringPointService meteringPointService;

    private final IMeteringPointCostDataHourService meteringPointCostDataHourService;
    private final IMeteringPointCostDataDayService meteringPointCostDataDayService;
    private final IMeteringPointCostDataMonthService meteringPointCostDataMonthService;

    private final DateTimeFormatter filedForMatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Override
    public Table findDay(String energyFlowDiagramIds, LocalDate localDate) {
        List<MeteringPoint> configs = meteringPointService.getByIds(strToLongList(energyFlowDiagramIds));
        List<Long> configIds = configs.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        List<TableHeader> tableHeaderList = TableUtil.dayHeaders(localDate);
        List<? extends MeteringPointCostData> meterDataList = meteringPointCostDataHourService.findByTimeRangeAndPointIds(
                LocalDateTime.of(localDate, LocalTime.MIN),
                LocalDateTime.of(localDate, LocalTime.MIN.withHour(23)),
                configIds
        );
        return createTable(tableHeaderList, configs, meterDataList);
    }

    @Override
    public Table findMonth(String energyFlowDiagramIds, LocalDate localDate) {
        List<MeteringPoint> configs = meteringPointService.getByIds(strToLongList(energyFlowDiagramIds));
        List<Long> configIds = configs.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        List<TableHeader> tableHeaderList = TableUtil.monthHeaders(localDate.getYear(), localDate.getMonthValue());
        List<? extends MeteringPointCostData> meterDataList = meteringPointCostDataDayService.findByTimeRangeAndPointIds(
                LocalDateTime.of(localDate.withDayOfMonth(1), LocalTime.MIN),
                LocalDateTime.of(localDate.withDayOfMonth(1).plusMonths(1), LocalTime.MIN),
                configIds
        );
        return createTable(tableHeaderList, configs, meterDataList);
    }

    @Override
    public Table findYear(String energyFlowDiagramIds, LocalDate localDate) {
        List<MeteringPoint> configs = meteringPointService.getByIds(strToLongList(energyFlowDiagramIds));
        List<Long> configIds = configs.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        List<TableHeader> tableHeaderList = TableUtil.yearHeaders(localDate.getYear());
        List<? extends MeteringPointCostData> meterDataList = meteringPointCostDataMonthService.findByTimeRangeAndPointIds(
                LocalDateTime.of(localDate.withMonth(1).withDayOfMonth(1), LocalTime.MIN),
                LocalDateTime.of(localDate.withMonth(1).withDayOfMonth(1).plusYears(1), LocalTime.MIN),
                configIds
        );
        return createTable(tableHeaderList, configs, meterDataList);
    }

    /**
     * 获取当月总成本
     * @param pointIds   计量点位集合
     * @return 成本
     */
    @Override
    public CostVo getTotalCost(LocalDate date, List<Long> pointIds) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        List<MeteringPointCostDataMonth> dataList = meteringPointCostDataMonthService.findByTimeAndPointIds(date.atStartOfDay(), pointIds);
        for (MeteringPointCostDataMonth item : dataList) {
            if(item != null && item.getValue() != null && item.getCost() != null){
                total = total.add(item.getValue());
                cost = cost.add(item.getCost());
            }
        }
        return new CostVo(total, cost);
    }

    /**
     * 获取各专业成本饼状图
     *
     * @param date       月份
     * @param pointIds   计量点位集合
     */
    @Override
    public PieChat findSpecialtyPieChat(LocalDate date, List<Long> pointIds) {
        List<PieChatSeriesData> seriesData = new ArrayList<>();
        PieChat pieChat = new PieChat("各专业成本", seriesData);
        // 获取各点位信息
        List<MeteringPoint> points = meteringPointService.getByIds(pointIds);
        // 获取点位成本数据
        Map<Long,BigDecimal> dataMap = meteringPointCostDataMonthService.findByTimeAndPointIds(date.atStartOfDay(), pointIds)
                .stream()
                .collect(Collectors.groupingBy(MeteringPointCostDataMonth::getMeteringPointId,
                        Collectors.mapping(MeteringPointCostDataMonth::getCost,
                                Collectors.reducing(BigDecimal.ZERO,BigDecimal::add))));
        for (MeteringPoint point : points) {
            seriesData.add(new PieChatSeriesData(point.getNodeName(),"",dataMap.getOrDefault(point.getId(),BigDecimal.ZERO),"元"));
        }
        return pieChat;
    }

    /**
     * 获取每日成本
     *
     * @param date       月份
     * @param pointIds   计量点位集合
     * @return
     */
    @Override
    public Chat findDayCost(LocalDate date, List<Long> pointIds) {

        // 获取横坐标
        List<String> xAxis = IntStream.range(1, date.lengthOfMonth() + 1).mapToObj(String::valueOf).collect(Collectors.toList());
        List<ChatSeries> series = new ArrayList<>();
        List<Object> data = new ArrayList<>();
        series.add(new ChatSeries("", data));
        Chat chat = new Chat("每日成本", xAxis, series, null);
        Map<String,BigDecimal> dataMap = meteringPointCostDataDayService.findByTimeRangeAndPointIds(date.withDayOfMonth(1).atStartOfDay(), date.withDayOfMonth(date.lengthOfMonth()).atTime(LocalTime.MAX), pointIds)
                .stream()
                .collect(Collectors.groupingBy(item -> String.valueOf(item.getTime().getDayOfMonth()),
                        Collectors.mapping(MeteringPointCostDataDay::getCost,
                                Collectors.reducing(BigDecimal.ZERO,BigDecimal::add))));
        for (String day : xAxis) {
            data.add(dataMap.getOrDefault(day, BigDecimal.ZERO));
        }
        return chat;
    }


    /**
     * 峰谷分时计价成本计算
     *
     * @param pvts     峰谷分时价格
     * @param dataList 小时能耗数据
     * @return 成本
     */
    private BigDecimal calculationPVTS(Map<String, BigDecimal> pvts, List<MeteringPointDataHour> dataList) {
        BigDecimal cost = BigDecimal.ZERO;
        for (MeteringPointDataHour pointData : dataList) {
            String format = pointData.getTime().format(EnergyPricingConfig.filedForMatter);
            BigDecimal price = pvts.get(format);
            if (price != null) {
                cost = cost.add(pointData.getValue().multiply(price));
            }
        }
        return cost;
    }

    private Table createTable(List<TableHeader> tableHeaderList, List<MeteringPoint> configs, List<? extends MeteringPointCostData> meterDataList) {
        Map<Long, Map<LocalDateTime, BigDecimal>> dataMap = meterDataList.stream()
                .collect(Collectors.groupingBy(MeteringPointCostData::getMeteringPointId,
                        Collectors.toMap(MeteringPointCostData::getTime, MeteringPointCostData::getCost)));
        List<TableData> tableDataList = new ArrayList<>();
        // 表格尾行合计
        TableData sum = new TableData();
        sum.put("name", "合计");
        sum.put("sum", BigDecimal.ZERO);
        for (MeteringPoint config : configs) {
            TableData tableData = new TableData();
            Map<LocalDateTime, BigDecimal> dateTimeBigDecimalMap = dataMap.get(config.getId());
            for (TableHeader header : tableHeaderList) {
                String field = header.getField();
                if (field.equals("sum")) {
                    continue;
                }
                if (field.equals("name")) {
                    tableData.put(field, config.getNodeName());
                    continue;
                }
                LocalDateTime localDateTime = LocalDateTime.parse(field, filedForMatter);
                if (!sum.containsKey(field)) {
                    sum.put(field, BigDecimal.ZERO);
                }
                BigDecimal value = dateTimeBigDecimalMap == null ? BigDecimal.ZERO : dateTimeBigDecimalMap.getOrDefault(localDateTime, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                tableData.put(field, value);
                sum.put(field, ((BigDecimal) sum.get(field)).add(value));
            }
            tableData.calculateSum();
            sum.put("sum", ((BigDecimal) sum.get("sum")).add((BigDecimal) tableData.get("sum")));
            tableDataList.add(tableData);
        }
        tableDataList.add(sum);
        Table table = new Table();
        table.setTableHeaderList(tableHeaderList);
        table.setTableDataList(tableDataList);
        return table;
    }

    private List<Long> strToLongList(String str) {
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}
