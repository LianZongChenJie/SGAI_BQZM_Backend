package org.jeecg.modules.bems.energyAnalysis.service.impl;

import lombok.AllArgsConstructor;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.energyAnalysis.entity.*;
import org.jeecg.modules.bems.energyAnalysis.service.*;
import org.jeecg.modules.bems.energyAnalysis.util.pricing.CostCalculationUtil;
import org.jeecg.modules.bems.energyAnalysis.util.pricing.LadderPricing;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class MeteringPointCostDataServiceImpl implements IMeteringPointCostDataService {

    private final IMeteringPointService meteringPointService;

    private final IEnergyPricingConfigService energyPricingConfigService;

    private final IMeteringPointDataHourService meteringPointDataHourService;

    private final IMeteringPointCostDataHourService meteringPointCostDataHourService;

    private final IMeteringPointCostDataDayService meteringPointCostDataDayService;

    private final IMeteringPointCostDataMonthService meteringPointCostDataMonthService;

    private final IMeteringPointCostDataYearService meteringPointCostDataYearService;

    /**
     * 成本计算
     *
     * @param pointId 计量点位id
     * @param hour    小时
     * @param value   能耗值
     */
    @Override
    public void calculationCost(Long pointId, LocalDateTime hour, BigDecimal value) {
        // 获取点位信息
        MeteringPoint point = meteringPointService.getById(pointId);
        if (point == null) {
            return;
        }
        Long categoryId = point.getCategoryId();
        EnergyPricingConfig config = energyPricingConfigService.getByCategoryId(categoryId);
        if (config == null) {
            return;
        }
        BigDecimal cost = BigDecimal.ZERO;
        switch (config.getBillingWay()) {
            case "1":
                // 峰谷分时计价
                Map<String, BigDecimal> pvts = config.formatPVTS();
                cost = pvts.getOrDefault(hour.format(EnergyPricingConfig.filedForMatter), BigDecimal.ZERO).multiply(value);
                break;
            case "2":
                // 固定计价
                cost = config.getFixedUnitPrice().multiply(value);
                break;
            case "3":
                // 阶梯计价
                List<LadderPricing> ladderPricings = config.formatLadderPricing();
                // 获取截止到上个小时能耗总量
                cost = CostCalculationUtil.calculationLadderPricing(ladderPricings, getHistoryDosage(pointId, hour), value);
                break;
            default:
                throw new JeecgBootException("未定义的能源价格计算方式！");
        }
        // 更新成本信息
        saveCost(pointId, hour, value, cost);
    }

    /**
     * 获取点位计费周期内历史用量
     * @param pointId 点位id
     * @param hour 小时
     * @return 用量
     */
    private BigDecimal getHistoryDosage(Long pointId,LocalDateTime hour){
        if(hour.getDayOfMonth() == 1 && hour.getHour() == 0){
            return BigDecimal.ZERO;
        }
        return meteringPointDataHourService.findByPointIdAndTimeRange(pointId,hour.withDayOfMonth(1).withHour(0),hour.minusHours(1))
                .stream()
                .map(MeteringPointDataHour::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void saveCost(Long pointId, LocalDateTime time, BigDecimal value, BigDecimal cost) {
        MeteringPointCostDataHour hour = meteringPointCostDataHourService.findByTimeAndPointId(time, pointId);
        if (hour == null) {
            hour = new MeteringPointCostDataHour();
            hour.setMeteringPointId(pointId);
            hour.setTime(time);
        }
        BigDecimal updValue = hour.getValue() == null ? value : value.subtract(hour.getValue());
        BigDecimal updCost = hour.getCost() == null ? cost : cost.subtract(hour.getCost());
        hour.setValue(value);
        hour.setCost(cost);
        meteringPointCostDataHourService.saveOrUpdate(hour);
        // 更新日数据、月数据、年数据
        MeteringPointCostDataDay day = meteringPointCostDataDayService.findByTimeAndPointId(time.withHour(0), pointId);
        if (day == null) {
            day = new MeteringPointCostDataDay();
            day.setMeteringPointId(pointId);
            day.setTime(time.withHour(0));
        }
        day.setValue(day.getValue() == null ? updValue : day.getValue().add(updValue));
        day.setCost(day.getCost() == null ? updCost : day.getCost().add(updCost));
        meteringPointCostDataDayService.saveOrUpdate(day);
        MeteringPointCostDataMonth month = meteringPointCostDataMonthService.findByTimeAndPointId(time.withDayOfMonth(1).withHour(0), pointId);
        if (month == null){
            month = new MeteringPointCostDataMonth();
            month.setMeteringPointId(pointId);
            month.setTime(time.withDayOfMonth(1).withHour(0));
        }
        month.setValue(month.getValue() == null ? updValue : month.getValue().add(updValue));
        month.setCost(month.getCost() == null ? updCost : month.getCost().add(updCost));
        meteringPointCostDataMonthService.saveOrUpdate(month);
        MeteringPointCostDataYear year = meteringPointCostDataYearService.findByTimeAndPointId(time.withDayOfMonth(1).withMonth(1).withHour(0), pointId);
        if (year == null) {
            year = new MeteringPointCostDataYear();
            year.setMeteringPointId(pointId);
            year.setTime(time.withDayOfYear(1).withHour(0));
        }
        year.setValue(year.getValue() == null ? updValue : year.getValue().add(updValue));
        year.setCost(year.getCost() == null ? updCost : year.getCost().add(updCost));
        meteringPointCostDataYearService.saveOrUpdate(year);
    }


}
