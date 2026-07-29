package org.jeecg.modules.bems.homePage.service;

import lombok.AllArgsConstructor;
import org.jeecg.modules.bems.alarm.entity.AlarmRecord;
import org.jeecg.modules.bems.alarm.service.IAlarmRecordService;
import org.jeecg.modules.bems.dataBoard.vo.StatisticsVo;
import org.jeecg.modules.bems.homePage.dto.AlarmStatisticsDto;
import org.jeecg.modules.bems.mdm.entity.EquipmentCategory;
import org.jeecg.modules.bems.mdm.service.IEquipmentCategoryService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AlarmStatisticsService {

    private final IAlarmRecordService alarmRecordService;

    private final IEquipmentCategoryService equipmentCategoryService;

    /**
     * 告警数量统计-当日
     * @return
     */
    public List<AlarmStatisticsDto> alarmStatisticsForDay() {
        List<AlarmRecord> alarmRecords = alarmRecordService.listByAlarmTimeRange(LocalDate.now().atStartOfDay(), LocalDateTime.now());
        return alarmStatistics(alarmRecords);
    }

    /**
     * 告警数量统计-当月
     * @return
     */
    public List<AlarmStatisticsDto> alarmStatisticsForMonth() {
        List<AlarmRecord> alarmRecords = alarmRecordService.listByAlarmTimeRange(LocalDate.now().withDayOfMonth(1).atStartOfDay(), LocalDateTime.now());
        return alarmStatistics(alarmRecords);
    }

    /**
     * 告警数量统计-当年
     * @return
     */
    public List<AlarmStatisticsDto> alarmStatisticsForYear() {
        List<AlarmRecord> alarmRecords = alarmRecordService.listByAlarmTimeRange(LocalDate.now().withDayOfYear(1).atStartOfDay(), LocalDateTime.now());
        return alarmStatistics(alarmRecords);
    }

    /**
     * 告警统计-当月
     * @return 本月报警次数、同比、环比
     */
    public StatisticsVo alarmStatistics(){
        StatisticsVo statisticsVo = new StatisticsVo();
        LocalDateTime now = LocalDateTime.now();
        // 获取当月报警数量
        Long count = alarmRecordService.countByAlarmTimeRange(now.withDayOfMonth(1).toLocalDate().atStartOfDay(), now);
        // 获取上月报警数量
        Long lastMonth = alarmRecordService.countByAlarmTimeRange(now.minusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay(), now.minusMonths(1));
        // 获取上年当月报警数量
        Long lastYear = alarmRecordService.countByAlarmTimeRange(now.minusYears(1).withDayOfMonth(1).toLocalDate().atStartOfDay(),now.minusYears(1));
        // 计算同比
        Double mom = count.doubleValue() / lastMonth.doubleValue();
        // 获取环比
        Double yoy = count.doubleValue() / lastYear.doubleValue();
        statisticsVo.setName("当月报警数量");
        statisticsVo.setValue(count.toString());
        statisticsVo.setMom(statisticsVo.rate(new BigDecimal(lastMonth), new BigDecimal(count)));
        statisticsVo.setYoy(statisticsVo.rate(new BigDecimal(lastYear),new BigDecimal( count)));
        return statisticsVo;
    }

    /**
     * 告警数量统计
     * @param records 告警记录
     * @return
     */
    private List<AlarmStatisticsDto> alarmStatistics(List<AlarmRecord> records){
        // 获取设备类别信息
        Map<Long,String> list = equipmentCategoryService.list()
                .stream()
                .collect(Collectors.toMap(EquipmentCategory::getId, EquipmentCategory::getCategoryName));
        Map<Long, AlarmStatisticsDto> data = new HashMap<>();
        for(AlarmRecord item : records){
            Long deviceCategoryId = item.getDeviceCategoryId();
            AlarmStatisticsDto orDefault = data.getOrDefault(deviceCategoryId, new AlarmStatisticsDto(deviceCategoryId, list.get(deviceCategoryId)));
            orDefault.setTotal(orDefault.getTotal() + 1);
            if(item.getAlarmStatus().equals(AlarmRecord.ALARM_STATUS_UNTREATED)){
                orDefault.setUnprocessed(orDefault.getUnprocessed() + 1);
            }else{
                orDefault.setProcessed(orDefault.getProcessed() + 1);
            }
            data.put(deviceCategoryId, orDefault);
        }
        return data.values().stream().sorted(Comparator.comparing(AlarmStatisticsDto::getTotal).reversed()).collect(Collectors.toList());
    }
}
