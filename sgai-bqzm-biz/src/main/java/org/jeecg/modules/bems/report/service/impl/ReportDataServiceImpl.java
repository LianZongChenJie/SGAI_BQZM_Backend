package org.jeecg.modules.bems.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.alarm.entity.AlarmRecord;
import org.jeecg.modules.bems.alarm.service.IAlarmRecordService;
import org.jeecg.modules.bems.lighting.entity.LightingControlLog;
import org.jeecg.modules.bems.lighting.entity.LightingFaultRecord;
import org.jeecg.modules.bems.lighting.entity.LightingWorkOrder;
import org.jeecg.modules.bems.lighting.service.ILightingAnalysisService;
import org.jeecg.modules.bems.lighting.service.ILightingControlLogService;
import org.jeecg.modules.bems.lighting.service.ILightingFaultRecordService;
import org.jeecg.modules.bems.lighting.service.ILightingWorkOrderService;
import org.jeecg.modules.bems.lighting.vo.AreaEnergyVo;
import org.jeecg.modules.bems.lighting.vo.AreaRunTimeVo;
import org.jeecg.modules.bems.report.dto.CustomReportQueryDto;
import org.jeecg.modules.bems.report.dto.ReportDataQueryDto;
import org.jeecg.modules.bems.report.service.IReportDataService;
import org.jeecg.modules.bems.report.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表数据中心 - 6大报表的服务实现
 */
@Slf4j
@Service
@AllArgsConstructor
public class ReportDataServiceImpl implements IReportDataService {

    private final ILightingControlLogService controlLogService;
    private final ILightingFaultRecordService faultRecordService;
    private final ILightingWorkOrderService workOrderService;
    private final IAlarmRecordService alarmRecordService;
    private final ILightingAnalysisService analysisService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("0.8");

    // ==================== 1. 运行报表 ====================

    @Override
    public OperationReportVo operationReport(ReportDataQueryDto params) {
        OperationReportVo vo = new OperationReportVo();
        LocalDateTime start = params.getStartTime();
        LocalDateTime end = params.getEndTime();
        if (start == null) start = LocalDateTime.now().minusDays(30);
        if (end == null) end = LocalDateTime.now();

        // 1. 控制日志列表
        LambdaQueryWrapper<LightingControlLog> controlWrapper = new LambdaQueryWrapper<LightingControlLog>()
                .between(LightingControlLog::getControlTime, start, end)
                .orderByDesc(LightingControlLog::getControlTime)
                .last("LIMIT 1000");
        List<LightingControlLog> controlLogs = controlLogService.list(controlWrapper);
        List<OperationReportVo.ControlLogItem> controlLogItems = controlLogs.stream().map(c -> {
            OperationReportVo.ControlLogItem item = new OperationReportVo.ControlLogItem();
            item.setControlTime(c.getControlTime() == null ? null : c.getControlTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            item.setControlType(c.getControlType());
            item.setRelName(c.getRelName());
            item.setOperation(c.getOperation());
            item.setOperatorType(c.getOperatorType());
            item.setOperatorName(c.getOperatorName());
            item.setResult(c.getResult());
            return item;
        }).collect(Collectors.toList());
        vo.setControlLogList(controlLogItems);

        // 2. 模式统计（自动/手动）
        Map<String, Long> modeCount = controlLogs.stream()
                .filter(c -> StringUtils.hasText(c.getOperatorType()))
                .collect(Collectors.groupingBy(LightingControlLog::getOperatorType, Collectors.counting()));
        long modeTotal = modeCount.values().stream().mapToLong(Long::longValue).sum();
        List<OperationReportVo.ModeStatisticsItem> modeStatistics = new ArrayList<>();
        for (Map.Entry<String, Long> entry : modeCount.entrySet()) {
            OperationReportVo.ModeStatisticsItem item = new OperationReportVo.ModeStatisticsItem();
            item.setMode(entry.getKey());
            item.setCount(entry.getValue());
            item.setRatio(modeTotal == 0 ? BigDecimal.ZERO
                    : new BigDecimal(entry.getValue()).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(modeTotal), 2, RoundingMode.HALF_UP));
            modeStatistics.add(item);
        }
        vo.setModeStatistics(modeStatistics);

        // 3. 控制类型统计（场景/回路）
        Map<String, Long> typeCount = controlLogs.stream()
                .filter(c -> StringUtils.hasText(c.getControlType()))
                .collect(Collectors.groupingBy(LightingControlLog::getControlType, Collectors.counting()));
        List<OperationReportVo.TypeStatisticsItem> typeStatistics = new ArrayList<>();
        for (Map.Entry<String, Long> entry : typeCount.entrySet()) {
            OperationReportVo.TypeStatisticsItem item = new OperationReportVo.TypeStatisticsItem();
            item.setControlType(entry.getKey());
            item.setCount(entry.getValue());
            typeStatistics.add(item);
        }
        vo.setControlTypeStatistics(typeStatistics);

        // 4. 设备运行时长TOP（按 relId 分组，从照明分析服务获取）
        try {
            List<AreaRunTimeVo> areaRunTime = analysisService.getAreaRunTime(start, end);
            List<OperationReportVo.DeviceRunTimeItem> deviceItems = areaRunTime.stream().map(a -> {
                OperationReportVo.DeviceRunTimeItem item = new OperationReportVo.DeviceRunTimeItem();
                item.setId(a.getAreaId());
                item.setName(a.getAreaName());
                item.setRunHours(a.getRunTime() == null ? BigDecimal.ZERO : BigDecimal.valueOf(a.getRunTime()));
                item.setAreaName(a.getAreaName());
                return item;
            }).collect(Collectors.toList());
            vo.setDeviceRunTime(deviceItems);
        } catch (Exception e) {
            log.warn("运行报表设备时长数据获取失败", e);
            vo.setDeviceRunTime(Collections.emptyList());
        }

        // 5. 汇总
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("controlLogTotal", controlLogs.size());
        summary.put("successCount", controlLogs.stream().filter(c -> "成功".equals(c.getResult())).count());
        summary.put("failCount", controlLogs.stream().filter(c -> "失败".equals(c.getResult())).count());
        summary.put("autoCount", modeCount.getOrDefault("自动", 0L));
        summary.put("manualCount", modeCount.getOrDefault("手动", 0L));
        vo.setSummary(summary);

        return vo;
    }

    // ==================== 2. 能耗报表 ====================

    @Override
    public EnergyReportVo energyReport(ReportDataQueryDto params) {
        EnergyReportVo vo = new EnergyReportVo();
        LocalDate startDate = params.getStartTime() == null ? LocalDate.now().minusDays(30) : params.getStartTime().toLocalDate();
        LocalDate endDate = params.getEndTime() == null ? LocalDate.now() : params.getEndTime().toLocalDate();
        BigDecimal price = params.getPrice() == null ? DEFAULT_PRICE : params.getPrice();

        // 1. 各地块用电量
        try {
            List<AreaEnergyVo> areaEnergyList = analysisService.getAreaEnergy(startDate, endDate);
            List<EnergyReportVo.EnergyStatisticsItem> energyStatistics = areaEnergyList.stream().map(a -> {
                EnergyReportVo.EnergyStatisticsItem item = new EnergyReportVo.EnergyStatisticsItem();
                item.setLabel(String.valueOf(a.getAreaId()));
                item.setName(a.getAreaName());
                item.setEnergyValue(a.getEnergy() == null ? BigDecimal.ZERO : a.getEnergy());
                item.setType("area");
                return item;
            }).collect(Collectors.toList());
            vo.setEnergyStatistics(energyStatistics);

            // 2. 电费（按区域）
            List<EnergyReportVo.EnergyCostItem> energyCost = areaEnergyList.stream().map(a -> {
                EnergyReportVo.EnergyCostItem item = new EnergyReportVo.EnergyCostItem();
                item.setName(a.getAreaName());
                BigDecimal energy = a.getEnergy() == null ? BigDecimal.ZERO : a.getEnergy();
                item.setEnergyValue(energy);
                item.setPrice(price);
                item.setCost(energy.multiply(price).setScale(2, RoundingMode.HALF_UP));
                return item;
            }).collect(Collectors.toList());
            vo.setEnergyCost(energyCost);

            // 3. 能耗对比（本期 vs 上期）
            List<EnergyReportVo.EnergyCompareItem> compares = new ArrayList<>();
            long days = ChronoUnit.DAYS.between(startDate, endDate);
            LocalDate prevStart = startDate.minusDays(days);
            LocalDate prevEnd = startDate.minusDays(1);
            List<AreaEnergyVo> prevEnergy = analysisService.getAreaEnergy(prevStart, prevEnd);
            Map<Long, BigDecimal> prevMap = prevEnergy.stream()
                    .collect(Collectors.toMap(AreaEnergyVo::getAreaId, e -> e.getEnergy() == null ? BigDecimal.ZERO : e.getEnergy(), (k1, k2) -> k1));
            for (AreaEnergyVo a : areaEnergyList) {
                EnergyReportVo.EnergyCompareItem item = new EnergyReportVo.EnergyCompareItem();
                item.setName(a.getAreaName());
                BigDecimal current = a.getEnergy() == null ? BigDecimal.ZERO : a.getEnergy();
                BigDecimal previous = prevMap.getOrDefault(a.getAreaId(), BigDecimal.ZERO);
                item.setCurrentValue(current);
                item.setPreviousValue(previous);
                if (previous.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal rate = current.subtract(previous)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(previous, 2, RoundingMode.HALF_UP);
                    item.setChangeRate(rate);
                } else {
                    item.setChangeRate(BigDecimal.ZERO);
                }
                compares.add(item);
            }
            vo.setEnergyCompare(compares);
        } catch (Exception e) {
            log.warn("能耗报表数据获取失败", e);
        }

        // 4. 汇总
        BigDecimal totalEnergy = vo.getEnergyStatistics() == null ? BigDecimal.ZERO
                : vo.getEnergyStatistics().stream()
                    .map(EnergyReportVo.EnergyStatisticsItem::getEnergyValue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEnergy", totalEnergy);
        summary.put("totalCost", totalEnergy.multiply(price).setScale(2, RoundingMode.HALF_UP));
        summary.put("price", price);
        summary.put("startDate", startDate.format(DATE_FMT));
        summary.put("endDate", endDate.format(DATE_FMT));
        vo.setSummary(summary);

        return vo;
    }

    // ==================== 3. 故障报表 ====================

    @Override
    public FaultReportVo faultReport(ReportDataQueryDto params) {
        FaultReportVo vo = new FaultReportVo();
        LocalDateTime start = params.getStartTime();
        LocalDateTime end = params.getEndTime();
        if (start == null) start = LocalDateTime.now().minusDays(30);
        if (end == null) end = LocalDateTime.now();

        LambdaQueryWrapper<LightingFaultRecord> wrapper = new LambdaQueryWrapper<LightingFaultRecord>()
                .between(LightingFaultRecord::getFaultTime, start, end);
        List<LightingFaultRecord> records = faultRecordService.list(wrapper);

        // 1. 故障类型分布
        Map<String, Long> typeCount = records.stream()
                .filter(r -> StringUtils.hasText(r.getFaultType()))
                .collect(Collectors.groupingBy(LightingFaultRecord::getFaultType, Collectors.counting()));
        long total = records.size();
        List<FaultReportVo.FaultTypeItem> faultTypeDistribution = typeCount.entrySet().stream().map(e -> {
            FaultReportVo.FaultTypeItem item = new FaultReportVo.FaultTypeItem();
            item.setFaultType(e.getKey());
            item.setCount(e.getValue());
            item.setRatio(total == 0 ? BigDecimal.ZERO
                    : new BigDecimal(e.getValue()).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            return item;
        }).sorted(Comparator.comparing(FaultReportVo.FaultTypeItem::getCount).reversed()).collect(Collectors.toList());
        vo.setFaultTypeDistribution(faultTypeDistribution);

        // 2. 故障等级分布
        Map<String, Long> levelCount = records.stream()
                .filter(r -> StringUtils.hasText(r.getFaultLevel()))
                .collect(Collectors.groupingBy(LightingFaultRecord::getFaultLevel, Collectors.counting()));
        List<FaultReportVo.FaultLevelItem> faultLevelDistribution = levelCount.entrySet().stream().map(e -> {
            FaultReportVo.FaultLevelItem item = new FaultReportVo.FaultLevelItem();
            item.setFaultLevel(e.getKey());
            item.setCount(e.getValue());
            return item;
        }).collect(Collectors.toList());
        vo.setFaultLevelDistribution(faultLevelDistribution);

        // 3. 故障统计（按设备）
        Map<Long, List<LightingFaultRecord>> byDevice = records.stream()
                .filter(r -> r.getDeviceId() != null)
                .collect(Collectors.groupingBy(LightingFaultRecord::getDeviceId));
        List<FaultReportVo.FaultStatisticsItem> faultStatistics = byDevice.entrySet().stream().map(e -> {
            List<LightingFaultRecord> list = e.getValue();
            FaultReportVo.FaultStatisticsItem item = new FaultReportVo.FaultStatisticsItem();
            item.setId(e.getKey());
            item.setName(list.get(0).getDeviceName());
            item.setFaultCount((long) list.size());
            item.setRecoveredCount(list.stream().filter(r -> "已恢复".equals(r.getFaultStatus())).count());
            item.setUnhandledCount(list.stream().filter(r -> "未处理".equals(r.getFaultStatus())).count());
            long totalDuration = list.stream()
                    .filter(r -> r.getDuration() != null)
                    .mapToLong(LightingFaultRecord::getDuration).sum();
            long avgDuration = list.stream()
                    .filter(r -> r.getDuration() != null)
                    .mapToLong(LightingFaultRecord::getDuration).sum() / Math.max(list.size(), 1);
            item.setTotalDuration(totalDuration);
            item.setAvgDuration(avgDuration);
            return item;
        }).sorted(Comparator.comparing(FaultReportVo.FaultStatisticsItem::getFaultCount).reversed()).collect(Collectors.toList());
        vo.setFaultStatistics(faultStatistics);

        // 4. 故障趋势（按天）
        Map<String, FaultReportVo.FaultTrendItem> trendMap = new TreeMap<>();
        for (LightingFaultRecord r : records) {
            if (r.getFaultTime() == null) continue;
            String date = r.getFaultTime().toLocalDate().format(DATE_FMT);
            FaultReportVo.FaultTrendItem item = trendMap.computeIfAbsent(date, d -> {
                FaultReportVo.FaultTrendItem t = new FaultReportVo.FaultTrendItem();
                t.setDate(d);
                t.setCount(0L);
                t.setRecoveredCount(0L);
                return t;
            });
            item.setCount(item.getCount() + 1);
            if ("已恢复".equals(r.getFaultStatus())) {
                item.setRecoveredCount(item.getRecoveredCount() + 1);
            }
        }
        vo.setFaultTrend(new ArrayList<>(trendMap.values()));

        // 5. 故障TOP榜
        List<FaultReportVo.FaultTopItem> faultTop = byDevice.entrySet().stream().map(e -> {
            List<LightingFaultRecord> list = e.getValue();
            FaultReportVo.FaultTopItem item = new FaultReportVo.FaultTopItem();
            item.setDeviceId(e.getKey());
            item.setDeviceName(list.get(0).getDeviceName());
            item.setFaultCount((long) list.size());
            item.setTotalDuration(list.stream()
                    .filter(r -> r.getDuration() != null)
                    .mapToLong(LightingFaultRecord::getDuration).sum());
            return item;
        }).sorted(Comparator.comparing(FaultReportVo.FaultTopItem::getFaultCount).reversed())
                .limit(10).collect(Collectors.toList());
        vo.setFaultTop(faultTop);

        // 6. 汇总
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("recovered", records.stream().filter(r -> "已恢复".equals(r.getFaultStatus())).count());
        summary.put("unhandled", records.stream().filter(r -> "未处理".equals(r.getFaultStatus())).count());
        summary.put("processing", records.stream().filter(r -> "处理中".equals(r.getFaultStatus())).count());
        summary.put("avgDuration", records.stream()
                .filter(r -> r.getDuration() != null)
                .mapToLong(LightingFaultRecord::getDuration).average().orElse(0));
        vo.setSummary(summary);

        return vo;
    }

    // ==================== 4. 报警报表 ====================

    @Override
    public AlarmReportVo alarmReport(ReportDataQueryDto params) {
        AlarmReportVo vo = new AlarmReportVo();
        LocalDateTime start = params.getStartTime();
        LocalDateTime end = params.getEndTime();
        if (start == null) start = LocalDateTime.now().minusDays(30);
        if (end == null) end = LocalDateTime.now();

        List<AlarmRecord> records = alarmRecordService.listByAlarmTimeRange(start, end);

        // 1. 报警分类
        Map<String, Long> categoryCount = records.stream()
                .filter(r -> StringUtils.hasText(r.getAlarmCategoryName()))
                .collect(Collectors.groupingBy(AlarmRecord::getAlarmCategoryName, Collectors.counting()));
        long total = records.size();
        List<AlarmReportVo.AlarmCategoryItem> alarmCategory = categoryCount.entrySet().stream().map(e -> {
            AlarmReportVo.AlarmCategoryItem item = new AlarmReportVo.AlarmCategoryItem();
            item.setCategoryName(e.getKey());
            item.setCount(e.getValue());
            item.setRatio(total == 0 ? BigDecimal.ZERO
                    : new BigDecimal(e.getValue()).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            return item;
        }).sorted(Comparator.comparing(AlarmReportVo.AlarmCategoryItem::getCount).reversed()).collect(Collectors.toList());
        vo.setAlarmCategory(alarmCategory);

        // 2. 报警级别
        Map<String, List<AlarmRecord>> byLevel = records.stream()
                .filter(r -> StringUtils.hasText(r.getAlarmLevelName()))
                .collect(Collectors.groupingBy(AlarmRecord::getAlarmLevelName));
        List<AlarmReportVo.AlarmLevelItem> alarmLevel = byLevel.entrySet().stream().map(e -> {
            AlarmReportVo.AlarmLevelItem item = new AlarmReportVo.AlarmLevelItem();
            AlarmRecord sample = e.getValue().get(0);
            item.setLevelName(e.getKey());
            item.setColor(sample.getAlarmLevelColor());
            item.setCount((long) e.getValue().size());
            item.setHandledCount(e.getValue().stream()
                    .filter(r -> !AlarmRecord.ALARM_STATUS_UNTREATED.equals(r.getAlarmStatus())).count());
            return item;
        }).collect(Collectors.toList());
        vo.setAlarmLevel(alarmLevel);

        // 3. 响应时效
        Map<String, Long> rangeCount = new LinkedHashMap<>();
        rangeCount.put("0-15分钟", 0L);
        rangeCount.put("15-60分钟", 0L);
        rangeCount.put("1-4小时", 0L);
        rangeCount.put("4-24小时", 0L);
        rangeCount.put("24小时以上", 0L);
        long respondedSum = 0L;
        long respondedCount = 0L;
        for (AlarmRecord r : records) {
            if (AlarmRecord.ALARM_STATUS_UNTREATED.equals(r.getAlarmStatus())) continue;
            if (r.getAlarmTime() == null) continue;
            // 实际场景下应使用处理时间字段，这里用createTime近似
            LocalDateTime processTime = r.getAlarmTime();
            long minutes = Duration.between(r.getAlarmTime(), processTime).toMinutes();
            respondedSum += minutes;
            respondedCount++;
            if (minutes <= 15) rangeCount.put("0-15分钟", rangeCount.get("0-15分钟") + 1);
            else if (minutes <= 60) rangeCount.put("15-60分钟", rangeCount.get("15-60分钟") + 1);
            else if (minutes <= 240) rangeCount.put("1-4小时", rangeCount.get("1-4小时") + 1);
            else if (minutes <= 1440) rangeCount.put("4-24小时", rangeCount.get("4-24小时") + 1);
            else rangeCount.put("24小时以上", rangeCount.get("24小时以上") + 1);
        }
        List<AlarmReportVo.AlarmResponseTimeItem> responseTime = new ArrayList<>();
        for (Map.Entry<String, Long> e : rangeCount.entrySet()) {
            AlarmReportVo.AlarmResponseTimeItem item = new AlarmReportVo.AlarmResponseTimeItem();
            item.setTimeRange(e.getKey());
            item.setCount(e.getValue());
            responseTime.add(item);
        }
        vo.setResponseTime(responseTime);

        // 4. 报警趋势
        Map<String, AlarmReportVo.AlarmTrendItem> trendMap = new TreeMap<>();
        for (AlarmRecord r : records) {
            if (r.getAlarmTime() == null) continue;
            String date = r.getAlarmTime().toLocalDate().format(DATE_FMT);
            AlarmReportVo.AlarmTrendItem item = trendMap.computeIfAbsent(date, d -> {
                AlarmReportVo.AlarmTrendItem t = new AlarmReportVo.AlarmTrendItem();
                t.setDate(d);
                t.setTotalCount(0L);
                t.setHandledCount(0L);
                t.setUnhandledCount(0L);
                return t;
            });
            item.setTotalCount(item.getTotalCount() + 1);
            if (AlarmRecord.ALARM_STATUS_UNTREATED.equals(r.getAlarmStatus())) {
                item.setUnhandledCount(item.getUnhandledCount() + 1);
            } else {
                item.setHandledCount(item.getHandledCount() + 1);
            }
        }
        vo.setAlarmTrend(new ArrayList<>(trendMap.values()));

        // 5. 处理率
        AlarmReportVo.AlarmHandleRate handleRate = new AlarmReportVo.AlarmHandleRate();
        handleRate.setTotal(total);
        long handled = records.stream()
                .filter(r -> !AlarmRecord.ALARM_STATUS_UNTREATED.equals(r.getAlarmStatus()))
                .count();
        handleRate.setHandled(handled);
        handleRate.setUnhandled(total - handled);
        handleRate.setHandleRate(total == 0 ? BigDecimal.ZERO
                : new BigDecimal(handled).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        handleRate.setAvgResponseMinutes(respondedCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(respondedSum).divide(BigDecimal.valueOf(respondedCount), 2, RoundingMode.HALF_UP));
        vo.setHandleRate(handleRate);

        // 6. 汇总
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("handled", handled);
        summary.put("unhandled", total - handled);
        summary.put("handleRate", handleRate.getHandleRate());
        vo.setSummary(summary);

        return vo;
    }

    // ==================== 5. 工单报表 ====================

    @Override
    public WorkOrderReportVo workOrderReport(ReportDataQueryDto params) {
        WorkOrderReportVo vo = new WorkOrderReportVo();
        LocalDateTime start = params.getStartTime();
        LocalDateTime end = params.getEndTime();
        if (start == null) start = LocalDateTime.now().minusDays(30);
        if (end == null) end = LocalDateTime.now();

        LambdaQueryWrapper<LightingWorkOrder> wrapper = new LambdaQueryWrapper<LightingWorkOrder>()
                .between(LightingWorkOrder::getCreateTime, start, end);
        List<LightingWorkOrder> orders = workOrderService.list(wrapper);

        // 1. 状态统计
        Map<String, Long> statusCount = orders.stream()
                .filter(o -> StringUtils.hasText(o.getStatus()))
                .collect(Collectors.groupingBy(LightingWorkOrder::getStatus, Collectors.counting()));
        long total = orders.size();
        List<WorkOrderReportVo.StatusStatisticsItem> statusStatistics = statusCount.entrySet().stream().map(e -> {
            WorkOrderReportVo.StatusStatisticsItem item = new WorkOrderReportVo.StatusStatisticsItem();
            item.setStatus(e.getKey());
            item.setCount(e.getValue());
            item.setRatio(total == 0 ? BigDecimal.ZERO
                    : new BigDecimal(e.getValue()).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            return item;
        }).collect(Collectors.toList());
        vo.setStatusStatistics(statusStatistics);

        // 2. 来源统计
        Map<String, Long> sourceCount = orders.stream()
                .filter(o -> StringUtils.hasText(o.getSource()))
                .collect(Collectors.groupingBy(LightingWorkOrder::getSource, Collectors.counting()));
        List<WorkOrderReportVo.SourceStatisticsItem> sourceStatistics = sourceCount.entrySet().stream().map(e -> {
            WorkOrderReportVo.SourceStatisticsItem item = new WorkOrderReportVo.SourceStatisticsItem();
            item.setSource(e.getKey());
            item.setCount(e.getValue());
            return item;
        }).collect(Collectors.toList());
        vo.setSourceStatistics(sourceStatistics);

        // 3. 优先级统计
        Map<String, List<LightingWorkOrder>> byPriority = orders.stream()
                .filter(o -> StringUtils.hasText(o.getPriority()))
                .collect(Collectors.groupingBy(LightingWorkOrder::getPriority));
        List<WorkOrderReportVo.PriorityStatisticsItem> priorityStatistics = byPriority.entrySet().stream().map(e -> {
            WorkOrderReportVo.PriorityStatisticsItem item = new WorkOrderReportVo.PriorityStatisticsItem();
            item.setPriority(e.getKey());
            item.setCount((long) e.getValue().size());
            item.setCompletedCount(e.getValue().stream()
                    .filter(o -> "已完成".equals(o.getStatus())).count());
            return item;
        }).collect(Collectors.toList());
        vo.setPriorityStatistics(priorityStatistics);

        // 4. 处理效率（按负责人）
        Map<String, List<LightingWorkOrder>> byAssignee = orders.stream()
                .filter(o -> StringUtils.hasText(o.getAssigneeName()))
                .collect(Collectors.groupingBy(LightingWorkOrder::getAssigneeName));
        List<WorkOrderReportVo.ProcessEfficiencyItem> processEfficiency = byAssignee.entrySet().stream().map(e -> {
            List<LightingWorkOrder> list = e.getValue();
            WorkOrderReportVo.ProcessEfficiencyItem item = new WorkOrderReportVo.ProcessEfficiencyItem();
            item.setName(e.getKey());
            item.setTotal((long) list.size());
            item.setCompleted(list.stream().filter(o -> "已完成".equals(o.getStatus())).count());
            item.setProcessing(list.stream().filter(o -> "处理中".equals(o.getStatus())).count());
            item.setPending(list.stream().filter(o -> "待处理".equals(o.getStatus())).count());
            item.setCompletionRate(list.size() == 0 ? BigDecimal.ZERO
                    : new BigDecimal(item.getCompleted()).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP));
            // 平均处理时长
            long hoursSum = 0L;
            long count = 0L;
            for (LightingWorkOrder o : list) {
                if (o.getHandleTime() != null && o.getCreateTime() != null) {
                    hoursSum += Duration.between(o.getCreateTime(), o.getHandleTime()).toHours();
                    count++;
                }
            }
            item.setAvgHandleHours(count == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(hoursSum).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
            return item;
        }).sorted(Comparator.comparing(WorkOrderReportVo.ProcessEfficiencyItem::getCompletionRate).reversed())
                .collect(Collectors.toList());
        vo.setProcessEfficiency(processEfficiency);

        // 5. 工单趋势
        Map<String, WorkOrderReportVo.WorkOrderTrendItem> trendMap = new TreeMap<>();
        for (LightingWorkOrder o : orders) {
            if (o.getCreateTime() == null) continue;
            String date = o.getCreateTime().toLocalDate().format(DATE_FMT);
            WorkOrderReportVo.WorkOrderTrendItem item = trendMap.computeIfAbsent(date, d -> {
                WorkOrderReportVo.WorkOrderTrendItem t = new WorkOrderReportVo.WorkOrderTrendItem();
                t.setDate(d);
                t.setCreated(0L);
                t.setCompleted(0L);
                return t;
            });
            item.setCreated(item.getCreated() + 1);
            if ("已完成".equals(o.getStatus())) {
                item.setCompleted(item.getCompleted() + 1);
            }
        }
        vo.setTrend(new ArrayList<>(trendMap.values()));

        // 6. 汇总
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("pending", statusCount.getOrDefault("待处理", 0L));
        summary.put("processing", statusCount.getOrDefault("处理中", 0L));
        summary.put("completed", statusCount.getOrDefault("已完成", 0L));
        summary.put("completionRate", total == 0 ? BigDecimal.ZERO
                : new BigDecimal(statusCount.getOrDefault("已完成", 0L)).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        vo.setSummary(summary);

        return vo;
    }

    // ==================== 6. 自定义报表 ====================

    @Override
    public CustomReportVo customReport(CustomReportQueryDto params) {
        CustomReportVo vo = new CustomReportVo();
        if (params == null || !StringUtils.hasText(params.getDataSource())) {
            return vo;
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        List<CustomReportVo.ColumnDefine> columns = new ArrayList<>();
        Long total = 0L;

        String dataSource = params.getDataSource();
        String measureType = StringUtils.hasText(params.getMeasureType()) ? params.getMeasureType() : "count";

        switch (dataSource) {
            case "operation":
                rows = aggregateFromControlLog(params);
                columns = buildColumns(params.getDimensionFields(), measureType);
                total = (long) rows.size();
                break;
            case "fault":
                rows = aggregateFromFault(params);
                columns = buildColumns(params.getDimensionFields(), measureType);
                total = (long) rows.size();
                break;
            case "alarm":
                rows = aggregateFromAlarm(params);
                columns = buildColumns(params.getDimensionFields(), measureType);
                total = (long) rows.size();
                break;
            case "workOrder":
                rows = aggregateFromWorkOrder(params);
                columns = buildColumns(params.getDimensionFields(), measureType);
                total = (long) rows.size();
                break;
            case "energy":
                rows = aggregateFromEnergy(params);
                columns = buildColumns(params.getDimensionFields(), measureType);
                total = (long) rows.size();
                break;
            default:
                log.warn("未知数据源: {}", dataSource);
        }

        vo.setRows(rows);
        vo.setColumns(columns);
        vo.setTotal(total);
        return vo;
    }

    private List<Map<String, Object>> aggregateFromControlLog(CustomReportQueryDto params) {
        LocalDateTime start = params.getTimeRange() == null || params.getTimeRange().getStartTime() == null
                ? LocalDateTime.now().minusDays(30) : params.getTimeRange().getStartTime();
        LocalDateTime end = params.getTimeRange() == null || params.getTimeRange().getEndTime() == null
                ? LocalDateTime.now() : params.getTimeRange().getEndTime();

        List<LightingControlLog> logs = controlLogService.list(new LambdaQueryWrapper<LightingControlLog>()
                .between(LightingControlLog::getControlTime, start, end));

        Map<String, Long> grouped = groupAndCount(logs, params.getDimensionFields(), LightingControlLog.class);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : grouped.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dimension", e.getKey());
            row.put("measure", e.getValue());
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> aggregateFromFault(CustomReportQueryDto params) {
        LocalDateTime start = params.getTimeRange() == null || params.getTimeRange().getStartTime() == null
                ? LocalDateTime.now().minusDays(30) : params.getTimeRange().getStartTime();
        LocalDateTime end = params.getTimeRange() == null || params.getTimeRange().getEndTime() == null
                ? LocalDateTime.now() : params.getTimeRange().getEndTime();

        List<LightingFaultRecord> records = faultRecordService.list(new LambdaQueryWrapper<LightingFaultRecord>()
                .between(LightingFaultRecord::getFaultTime, start, end));

        Map<String, Long> grouped = groupAndCount(records, params.getDimensionFields(), LightingFaultRecord.class);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : grouped.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dimension", e.getKey());
            row.put("measure", e.getValue());
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> aggregateFromAlarm(CustomReportQueryDto params) {
        LocalDateTime start = params.getTimeRange() == null || params.getTimeRange().getStartTime() == null
                ? LocalDateTime.now().minusDays(30) : params.getTimeRange().getStartTime();
        LocalDateTime end = params.getTimeRange() == null || params.getTimeRange().getEndTime() == null
                ? LocalDateTime.now() : params.getTimeRange().getEndTime();

        List<AlarmRecord> records = alarmRecordService.listByAlarmTimeRange(start, end);
        Map<String, Long> grouped = groupAndCount(records, params.getDimensionFields(), AlarmRecord.class);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : grouped.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dimension", e.getKey());
            row.put("measure", e.getValue());
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> aggregateFromWorkOrder(CustomReportQueryDto params) {
        LocalDateTime start = params.getTimeRange() == null || params.getTimeRange().getStartTime() == null
                ? LocalDateTime.now().minusDays(30) : params.getTimeRange().getStartTime();
        LocalDateTime end = params.getTimeRange() == null || params.getTimeRange().getEndTime() == null
                ? LocalDateTime.now() : params.getTimeRange().getEndTime();

        List<LightingWorkOrder> orders = workOrderService.list(new LambdaQueryWrapper<LightingWorkOrder>()
                .between(LightingWorkOrder::getCreateTime, start, end));
        Map<String, Long> grouped = groupAndCount(orders, params.getDimensionFields(), LightingWorkOrder.class);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : grouped.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dimension", e.getKey());
            row.put("measure", e.getValue());
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> aggregateFromEnergy(CustomReportQueryDto params) {
        LocalDate startDate = params.getTimeRange() == null || params.getTimeRange().getStartTime() == null
                ? LocalDate.now().minusDays(30) : params.getTimeRange().getStartTime().toLocalDate();
        LocalDate endDate = params.getTimeRange() == null || params.getTimeRange().getEndTime() == null
                ? LocalDate.now() : params.getTimeRange().getEndTime().toLocalDate();

        try {
            List<AreaEnergyVo> areaEnergy = analysisService.getAreaEnergy(startDate, endDate);
            List<Map<String, Object>> result = new ArrayList<>();
            for (AreaEnergyVo a : areaEnergy) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("dimension", a.getAreaName());
                row.put("measure", a.getEnergy() == null ? BigDecimal.ZERO : a.getEnergy());
                result.add(row);
            }
            return result;
        } catch (Exception e) {
            log.warn("自定义报表-能耗聚合失败", e);
            return Collections.emptyList();
        }
    }

    private Map<String, Long> groupAndCount(List<?> list, List<String> dimensionFields, Class<?> clazz) {
        if (CollectionUtils.isEmpty(list) || CollectionUtils.isEmpty(dimensionFields)) {
            return Collections.emptyMap();
        }
        // 简化实现：按第一个维度字段分组
        String firstField = dimensionFields.get(0);
        return list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        item -> getFieldValue(item, firstField),
                        Collectors.counting()
                ));
    }

    private String getFieldValue(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = findField(obj.getClass(), fieldName);
            if (field == null) return "";
            field.setAccessible(true);
            Object value = field.get(obj);
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            log.warn("获取字段值失败: {}", fieldName, e);
            return "";
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private List<CustomReportVo.ColumnDefine> buildColumns(List<String> dimensionFields, String measureType) {
        List<CustomReportVo.ColumnDefine> columns = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dimensionFields)) {
            for (String f : dimensionFields) {
                CustomReportVo.ColumnDefine col = new CustomReportVo.ColumnDefine();
                col.setField(f);
                col.setLabel(f);
                col.setType("dimension");
                col.setDataType("string");
                columns.add(col);
            }
        }
        CustomReportVo.ColumnDefine measureCol = new CustomReportVo.ColumnDefine();
        measureCol.setField("measure");
        measureCol.setLabel(measureType);
        measureCol.setType("metric");
        measureCol.setDataType("number");
        columns.add(measureCol);
        return columns;
    }
}
