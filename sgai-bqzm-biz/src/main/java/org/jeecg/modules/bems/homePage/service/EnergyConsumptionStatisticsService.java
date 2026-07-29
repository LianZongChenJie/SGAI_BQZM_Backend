package org.jeecg.modules.bems.homePage.service;

import lombok.AllArgsConstructor;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataDay;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataHour;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataMonth;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataYear;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataDayService;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataHourService;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataMonthService;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataYearService;
import org.jeecg.modules.bems.homePage.dto.EnergyConsumptionStatisticsDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class EnergyConsumptionStatisticsService {

    private final IMeteringPointDataHourService meteringPointDataHourService;
    private final IMeteringPointDataDayService meteringPointDataDayService;
    private final IMeteringPointDataMonthService meteringPointDataMonthService;
    private final IMeteringPointDataYearService meteringPointDataYearService;

    /**
     * 日能耗统计
     * @param meteringPointId 计量点位id
     * @param day 日期
     * @return 能耗统计信息
     */
    public EnergyConsumptionStatisticsDto energyConsumptionStatisticsForDay(Long meteringPointId,LocalDate day){
        MeteringPointDataDay pointDataDay = meteringPointDataDayService.findByDateAndPointId(day, meteringPointId);
        BigDecimal value = pointDataDay == null ? BigDecimal.ZERO : pointDataDay.getValue();
        BigDecimal lastValue;
        BigDecimal lastLastValue;
        if (day.isEqual(LocalDate.now())){
            lastValue = meteringPointDataHourService.findByPointIdAndTimeRange(meteringPointId, day.minusDays(1).atStartOfDay(), LocalDateTime.now().minusDays(1))
                    .stream().map(MeteringPointDataHour::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            lastLastValue = meteringPointDataHourService.findByPointIdAndTimeRange(meteringPointId, day.minusYears(1).atStartOfDay(), LocalDateTime.now().minusYears(1))
                    .stream().map(MeteringPointDataHour::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        }else{
            MeteringPointDataDay pointDataLastDay = meteringPointDataDayService.findByDateAndPointId(day.minusDays(1), meteringPointId);
            lastValue = pointDataLastDay == null ? BigDecimal.ZERO : pointDataLastDay.getValue();
            MeteringPointDataDay pointDataLastLastDay = meteringPointDataDayService.findByDateAndPointId(day.minusYears(1), meteringPointId);
            lastLastValue = pointDataLastLastDay == null ? BigDecimal.ZERO : pointDataLastLastDay.getValue();
        }
        return EnergyConsumptionStatisticsDto.calculation(value, lastValue, lastLastValue);
    }

    /**
     * 月能耗统计
     * @param meteringPointId 计量点位id
     * @param date 时间
     * @return 能耗统计信息
     */
    public EnergyConsumptionStatisticsDto energyConsumptionStatisticsForMonth(Long meteringPointId, LocalDate date){
        MeteringPointDataMonth pointDataMonth = meteringPointDataMonthService.findByDateAndPointId(date, meteringPointId);
        BigDecimal value = pointDataMonth == null ? BigDecimal.ZERO : pointDataMonth.getValue();
        BigDecimal lastValue;
        BigDecimal lastLastValue;
        if(date.getMonth() == LocalDate.now().getMonth()){
            lastValue = meteringPointDataDayService.findByTimeRangeAndPointId(date.minusMonths(1).withDayOfMonth(1), LocalDate.now().minusMonths(1), meteringPointId)
                    .stream().map(MeteringPointDataDay::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);

            lastLastValue = meteringPointDataDayService.findByTimeRangeAndPointId(date.minusYears(1).withDayOfMonth(1), LocalDate.now().minusYears(1),meteringPointId)
                    .stream().map(MeteringPointDataDay::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        }else{
            MeteringPointDataMonth pointDataLastMonth = meteringPointDataMonthService.findByDateAndPointId(date.minusMonths(1), meteringPointId);
            lastValue = pointDataLastMonth == null ? BigDecimal.ZERO : pointDataLastMonth.getValue();
            MeteringPointDataMonth pointDataLastLastMonth = meteringPointDataMonthService.findByDateAndPointId(date.minusYears(1), meteringPointId);
            lastLastValue = pointDataLastLastMonth == null ? BigDecimal.ZERO : pointDataLastLastMonth.getValue();
        }
        return EnergyConsumptionStatisticsDto.calculation(value, lastValue, lastLastValue);
    }

    /**
     * 年能耗统计
     * @param meteringPointId 计量点位id
     * @param date 时间
     * @return 能耗统计信息
     */
    public EnergyConsumptionStatisticsDto energyConsumptionStatisticsForYear(Long meteringPointId, LocalDate date){
        MeteringPointDataYear pointDataYear = meteringPointDataYearService.findByDateAndPointId(date, meteringPointId);
        BigDecimal value = pointDataYear == null ? BigDecimal.ZERO : pointDataYear.getValue();
        BigDecimal lastValue;
        if(date.getYear() == LocalDate.now().getYear()){
            lastValue = meteringPointDataMonthService.findByTimeRangeAndPointId(date.minusYears(1).withMonth(1), LocalDate.now().minusYears(1), meteringPointId)
                    .stream().map(MeteringPointDataMonth::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        }else{
            MeteringPointDataYear pointDataLastYear = meteringPointDataYearService.findByDateAndPointId(date.minusYears(1), meteringPointId);
            lastValue = pointDataLastYear == null ? BigDecimal.ZERO : pointDataLastYear.getValue();
        }
        return EnergyConsumptionStatisticsDto.calculation(value, lastValue, lastValue);
    }

}
