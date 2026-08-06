package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.service.ILightingAnalysisService;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.vo.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 照明分析服务实现
 */
@Service
@AllArgsConstructor
@Slf4j
public class LightingAnalysisServiceImpl implements ILightingAnalysisService {

    private final ILightingAreaService areaService;
    private final ILightingCircuitService circuitService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ========== 运行时长 ==========

    @Override
    public List<AreaRunTimeVo> getAreaRunTime(LocalDateTime startTime, LocalDateTime endTime) {
        List<LightingArea> areas = areaService.list();
        List<LightingCircuit> circuits = circuitService.list();

        // 按区域分组统计回路运行时长
        Map<Long, Double> areaRunTimeMap = circuits.stream()
                .collect(Collectors.groupingBy(
                        LightingCircuit::getAreaId,
                        Collectors.summingDouble(c -> c.getTotalRunTime() != null ? c.getTotalRunTime() : 0.0)
                ));

        List<AreaRunTimeVo> result = new ArrayList<>();
        for (LightingArea area : areas) {
            AreaRunTimeVo vo = new AreaRunTimeVo();
            vo.setAreaId(area.getId());
            vo.setAreaName(area.getAreaName());
            vo.setRunTime(areaRunTimeMap.getOrDefault(area.getId(), 0.0));
            result.add(vo);
        }

        // 按运行时长倒序
        result.sort(Comparator.comparing(AreaRunTimeVo::getRunTime).reversed());
        return result;
    }

    @Override
    public List<AreaRunTimeCompareVo> getRunTimeCompare(List<Long> areaIds, LocalDateTime startTime, LocalDateTime endTime) {
        List<LightingArea> areas = (areaIds != null && !areaIds.isEmpty())
                ? areaService.listByIds(areaIds)
                : areaService.list();
        List<LightingCircuit> circuits = circuitService.list();

        // 回路按区域分组
        Map<Long, List<LightingCircuit>> circuitByArea = circuits.stream()
                .collect(Collectors.groupingBy(LightingCircuit::getAreaId));

        // 统计天数（用于平均时长，至少 1 天）
        LocalDateTime start = startTime != null ? startTime
                : LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        long days = Math.max(1, ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1);

        // 按地块（spaceName）分组累计
        Map<String, SpaceAgg> aggMap = new LinkedHashMap<>();
        for (LightingArea area : areas) {
            String spaceName = (area.getSpaceName() == null || area.getSpaceName().isEmpty())
                    ? area.getAreaName() : area.getSpaceName();
            String key = (area.getSpace() == null ? "" : area.getSpace()) + "|" + (spaceName == null ? "" : spaceName);
            SpaceAgg agg = aggMap.computeIfAbsent(key, k -> new SpaceAgg(area.getSpace(), spaceName));
            List<LightingCircuit> areaCircuits = circuitByArea.getOrDefault(area.getId(), Collections.emptyList());
            agg.circuitCount += areaCircuits.size();
            for (LightingCircuit circuit : areaCircuits) {
                agg.totalSeconds += (circuit.getAllDuration() != null ? circuit.getAllDuration() : 0L);
            }
        }

        List<AreaRunTimeCompareVo> result = new ArrayList<>();
        for (SpaceAgg agg : aggMap.values()) {
            AreaRunTimeCompareVo vo = new AreaRunTimeCompareVo();
            vo.setSpace(agg.space);
            vo.setSpaceName(agg.spaceName);
            vo.setCircuitCount((long) agg.circuitCount);
            double totalHours = agg.totalSeconds / 3600.0;
            vo.setTotalRunTime(round1(totalHours));
            double avgRunTime = agg.circuitCount > 0 ? totalHours / (agg.circuitCount * days) : 0.0;
            vo.setAvgRunTime(round1(avgRunTime));
            // 同比：当前系统无历史运行时长数据源（totalRunTime 未维护），暂无值，待接入历史数据后计算
            vo.setYoy(null);
            result.add(vo);
        }

        // 按总运行时长倒序
        result.sort(Comparator.comparing(AreaRunTimeCompareVo::getTotalRunTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    // ========== 用电量 ==========

    @Override
    public List<AreaEnergyVo> getAreaEnergy(LocalDate startDate, LocalDate endDate) {
        List<LightingArea> areas = areaService.list();
        List<AreaEnergyVo> result = new ArrayList<>();

        // TODO: 目前用今日用电量模拟，后续接历史能耗表
        // 生成最近7天的模拟数据
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            for (LightingArea area : areas) {
                AreaEnergyVo vo = new AreaEnergyVo();
                vo.setDate(date.toString());
                vo.setAreaId(area.getId());
                vo.setAreaName(area.getAreaName());
                // 模拟数据：今日用电量基础上随机波动
                double baseEnergy = area.getTodayEnergy() != null ? area.getTodayEnergy() : 100.0;
                double randomFactor = 0.8 + Math.random() * 0.4; // 0.8~1.2 波动
                vo.setEnergy(BigDecimal.valueOf(baseEnergy * randomFactor).setScale(2, RoundingMode.HALF_UP));
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public List<EnergyCostVo> getEnergyCost(LocalDate startDate, LocalDate endDate) {
        List<EnergyCostVo> result = new ArrayList<>();

        // 用电成本分类：峰、平、谷
        BigDecimal totalEnergy = BigDecimal.valueOf(5000); // 模拟总用电量
        BigDecimal peakPrice = BigDecimal.valueOf(1.2); // 峰电价
        BigDecimal flatPrice = BigDecimal.valueOf(0.8); // 平电价
        BigDecimal valleyPrice = BigDecimal.valueOf(0.4); // 谷电价

        BigDecimal peakEnergy = totalEnergy.multiply(BigDecimal.valueOf(0.4)); // 峰段40%
        BigDecimal flatEnergy = totalEnergy.multiply(BigDecimal.valueOf(0.35)); // 平段35%
        BigDecimal valleyEnergy = totalEnergy.multiply(BigDecimal.valueOf(0.25)); // 谷段25%

        BigDecimal peakCost = peakEnergy.multiply(peakPrice);
        BigDecimal flatCost = flatEnergy.multiply(flatPrice);
        BigDecimal valleyCost = valleyEnergy.multiply(valleyPrice);
        BigDecimal totalCost = peakCost.add(flatCost).add(valleyCost);

        result.add(buildCostVo("peak", "峰段电费", peakCost, totalCost));
        result.add(buildCostVo("flat", "平段电费", flatCost, totalCost));
        result.add(buildCostVo("valley", "谷段电费", valleyCost, totalCost));

        return result;
    }

    @Override
    public IPage<EnergyDetailVo> getEnergyDetail(int pageNo, int pageSize, Long areaId, LocalDate startDate, LocalDate endDate) {
        List<LightingCircuit> circuits = circuitService.list();
        List<EnergyDetailVo> allList = new ArrayList<>();

        for (LightingCircuit circuit : circuits) {
            if (areaId != null && !areaId.equals(circuit.getAreaId())) {
                continue;
            }
            EnergyDetailVo vo = new EnergyDetailVo();
            vo.setId(circuit.getId());
            vo.setAreaName(circuit.getAreaName());
            vo.setCircuitName(circuit.getCircuitName());
            vo.setDate(LocalDate.now().toString());
            vo.setEnergy(BigDecimal.valueOf(circuit.getTodayEnergy() != null ? circuit.getTodayEnergy() : 0.0)
                    .setScale(2, RoundingMode.HALF_UP));
            vo.setPrice(BigDecimal.valueOf(0.8)); // 模拟电价
            vo.setCost(vo.getEnergy().multiply(vo.getPrice()).setScale(2, RoundingMode.HALF_UP));
            vo.setStatTime(LocalDateTime.now());
            allList.add(vo);
        }

        // 手动分页
        int fromIndex = (pageNo - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allList.size());
        List<EnergyDetailVo> records = allList.subList(fromIndex, toIndex);

        Page<EnergyDetailVo> page = new Page<>(pageNo, pageSize);
        page.setRecords(records);
        page.setTotal(allList.size());
        return page;
    }

    // ========== 运行特性 ==========

    @Override
    public List<RunCharacteristicVo> getRunCharacteristic(LocalDate date) {
        List<RunCharacteristicVo> result = new ArrayList<>();
        List<LightingCircuit> circuits = circuitService.list();
        int total = circuits.size();

        // 生成24小时的模拟数据
        for (int hour = 0; hour < 24; hour++) {
            RunCharacteristicVo vo = new RunCharacteristicVo();
            vo.setTime(String.format("%02d:00", hour));

            // 模拟功率：白天高、夜间低
            double basePower = circuits.stream()
                    .mapToDouble(c -> c.getCurrentPower() != null ? c.getCurrentPower() : 0.0)
                    .sum();

            double factor;
            if (hour >= 6 && hour < 9) {
                factor = 0.3 + (hour - 6) * 0.2; // 早晨上升
            } else if (hour >= 9 && hour < 18) {
                factor = 0.9 + Math.random() * 0.1; // 白天高位
            } else if (hour >= 18 && hour < 22) {
                factor = 0.8 - (hour - 18) * 0.1; // 傍晚下降
            } else {
                factor = 0.2 + Math.random() * 0.1; // 夜间低位
            }

            vo.setPower(BigDecimal.valueOf(basePower * factor).setScale(2, RoundingMode.HALF_UP));

            // 模拟在线率
            double onlineRate = 90 + Math.random() * 10; // 90%~100%
            vo.setOnlineRate(BigDecimal.valueOf(onlineRate).setScale(1, RoundingMode.HALF_UP));

            result.add(vo);
        }
        return result;
    }

    @Override
    public List<NightRunModeVo> getNightRunMode(LocalDate startDate, LocalDate endDate) {
        List<NightRunModeVo> result = new ArrayList<>();
        String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        // 模拟一周的夜间运行时长（夜间定义为22:00~次日6:00，共8小时）
        for (int i = 0; i < 7; i++) {
            NightRunModeVo vo = new NightRunModeVo();
            vo.setWeekDay(weekDays[i]);
            // 模拟数据：工作日夜间运行短，周末长
            double baseTime = (i < 5) ? 4.5 : 6.0;
            double random = (Math.random() - 0.5) * 1.0; // ±0.5 波动
            vo.setNightRunTime(Math.round((baseTime + random) * 10) / 10.0);
            result.add(vo);
        }
        return result;
    }

    // ========== 私有方法 ==========

    private double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private EnergyCostVo buildCostVo(String type, String name, BigDecimal amount, BigDecimal total) {
        EnergyCostVo vo = new EnergyCostVo();
        vo.setCostType(type);
        vo.setCostName(name);
        vo.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        vo.setPercentage(total.compareTo(BigDecimal.ZERO) > 0
                ? amount.multiply(BigDecimal.valueOf(100))
                        .divide(total, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        return vo;
    }

    /**
     * 地块维度聚合（按 space+spaceName 分组）
     */
    private static class SpaceAgg {
        private final String space;
        private final String spaceName;
        private int circuitCount;
        private long totalSeconds;

        private SpaceAgg(String space, String spaceName) {
            this.space = space;
            this.spaceName = spaceName;
        }
    }
}
