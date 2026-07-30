package org.jeecg.modules.bems.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.alarm.dto.AlarmRecordDto;
import org.jeecg.modules.bems.alarm.dto.TransferEventDto;
import org.jeecg.modules.bems.alarm.entity.AlarmLevel;
import org.jeecg.modules.bems.alarm.entity.AlarmRecord;
import org.jeecg.modules.bems.alarm.entity.AlarmRulePoint;
import org.jeecg.modules.bems.alarm.entity.AlarmRules;
import org.jeecg.modules.bems.alarm.mapper.AlarmRecordMapper;
import org.jeecg.modules.bems.alarm.service.IAlarmLevelService;
import org.jeecg.modules.bems.alarm.service.IAlarmRecordService;
import org.jeecg.modules.bems.alarm.service.IAlarmRulePointService;
import org.jeecg.modules.bems.alarm.service.IAlarmRulesService;
import org.jeecg.modules.bems.alarm.vo.AlarmRecordStatisticsVo;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointData;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataYear;
import org.jeecg.modules.bems.energyAnalysis.service.*;
import org.jeecg.modules.bems.entity.DayData;
import org.jeecg.modules.bems.entity.HourData;
import org.jeecg.modules.bems.entity.MonthData;
import org.jeecg.modules.bems.entity.YearData;
import org.jeecg.modules.bems.event.dto.BusinessDataDto;
import org.jeecg.modules.bems.event.service.EventService;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.entity.DeviceAttribute;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeService;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.service.IDayDataService;
import org.jeecg.modules.bems.service.IHourDataService;
import org.jeecg.modules.bems.service.IMonthDataService;
import org.jeecg.modules.bems.service.IYearDataService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class AlarmRecordServiceImpl extends ServiceImpl<AlarmRecordMapper, AlarmRecord> implements IAlarmRecordService {
    private final IAlarmLevelService alarmLevelService;
    private final IAlarmRulePointService alarmRulePointService;
    private final IAlarmRulesService alarmRulesService;
    private final IDeviceService deviceService;
    private final IDeviceAttributeService deviceAttributeService;

    private final IHourDataService hourDataService;
    private final IDayDataService dayDataService;
    private final IMonthDataService monthDataService;
    private final IYearDataService yearDataService;

    private final IMeteringPointService meteringPointService;
    private final IMeteringPointDataHourService meteringPointDataHourService;
    private final IMeteringPointDataDayService meteringPointDataDayService;
    private final IMeteringPointDataMonthService meteringPointDataMonthService;
    private final IMeteringPointDataYearService meteringPointDataYearService;

    private final EventService eventService;

    @Override
    public IPage<AlarmRecord> listPage(AlarmRecordDto params) {
        return page(
                new Page<>(params.getPageNo(), params.getPageSize()),
                getQueryWrapper(params)
                        .ne(AlarmRecord::getAlarmStatus, AlarmRecord.ALARM_STATUS_TREATED)
                        .orderByDesc(AlarmRecord::getAlarmTime)
        );
    }

    /**
     * 报警消除
     *
     * @param id 报警记录id
     */
    @Override
    public void elimination(Long id) {
        update(new LambdaUpdateWrapper<AlarmRecord>().set(AlarmRecord::getAlarmStatus, AlarmRecord.ALARM_STATUS_TREATED)
                .eq(AlarmRecord::getAlarmStatus, AlarmRecord.ALARM_STATUS_UNTREATED)
                .eq(AlarmRecord::getId, id));
    }

    @Override
    public List<AlarmRecordStatisticsVo> levelStatistics(AlarmRecordDto params) {
        List<AlarmRecordStatisticsVo> result = new ArrayList<>();
        List<AlarmRecord> list = list(getQueryWrapper(params).select(AlarmRecord::getId, AlarmRecord::getAlarmLevelId));
        // 获取报警级别列表
        List<AlarmLevel> levels = alarmLevelService.list();
        Map<Long, Long> collect = list.stream().collect(Collectors.groupingBy(AlarmRecord::getAlarmLevelId, Collectors.counting()));
        for (AlarmLevel level : levels) {
            result.add(AlarmRecordStatisticsVo.create(level, collect.getOrDefault(level.getId(), 0L)));
        }
        return result;
    }

    /**
     * 查询时间范围内的告警记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 告警记录
     */
    @Override
    public List<AlarmRecord> listByAlarmTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return list(new LambdaQueryWrapper<AlarmRecord>()
                .select(AlarmRecord::getId,
                        AlarmRecord::getAlarmTime,
                        AlarmRecord::getAlarmLevelId,
                        AlarmRecord::getDeviceId,
                        AlarmRecord::getDeviceCategoryId,
                        AlarmRecord::getAlarmCategoryId,
                        AlarmRecord::getAlarmStatus)
                .between(AlarmRecord::getAlarmTime, startTime, endTime)
        );
    }

    /**
     * 查询时间范围内的告警记录数量
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 告警记录数量
     */
    @Override
    public Long countByAlarmTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return count(
                new LambdaQueryWrapper<AlarmRecord>()
                        .between(AlarmRecord::getAlarmTime, startTime, endTime)
        );
    }

    /**
     * 设备告警检测
     *
     * @param deviceId 设备id
     * @param pointId  点位id
     * @param value    点位值
     */
    @Override
    public void alarmDetection(Long deviceId, Long pointId, String value) {
        // 判断设备点位是否存在告警规则
        List<AlarmRulePoint> alarmRulePoints = alarmRulePointService.getByDeviceIdAndPointId(deviceId, pointId);
        if(CollectionUtils.isEmpty(alarmRulePoints)){
            return;
        }
        DeviceAttribute attribute = deviceAttributeService.getById(pointId);
        for (AlarmRulePoint alarmRulePoint : alarmRulePoints) {
            alarmRulePoint.setPointName(attribute.getAttributeName());
        }
        detection(deviceId, alarmRulePoints, AlarmRules.POINT_TYPE_INSTANT, value);
    }

    /**
     * 设备告警检测
     *
     * @param deviceId         设备id
     * @param deviceAttributes 设备点位
     */
    @Override
    public void alarmDetection(Long deviceId, List<DeviceAttribute> deviceAttributes) {
        
    }

    /**
     * 设备告警检测
     *
     * @param deviceId        设备id
     * @param timeGranularity 时间粒度
     * @param value           值
     */
    @Override
    public void alarmDetection(Long deviceId, String timeGranularity, String value) {
        log.info("设备告警检测。设备id: {}, 时间粒度: {}, 值: {}", deviceId, timeGranularity, value);
        List<AlarmRulePoint> alarmRulePoints = alarmRulePointService.getByDeviceIdAndTimeGranularity(deviceId, timeGranularity);
        detection(deviceId, alarmRulePoints, AlarmRules.POINT_TYPE_ACCUMULATE, value);
    }

    /**
     * 设备告警检测-累计值
     *
     * @param deviceId 设备id
     * @param hour     小时
     */
    @Override
    public void alarmDetection(Long deviceId, LocalDateTime hour) {
        if(deviceId == null || hour == null){
            return;
        }
        Device device = deviceService.getDetail(deviceId);
        if (device == null) {
            return;
        }

        // 获取开启的报警条件
        List<AlarmRules> rules = alarmRulesService.list()
                .stream()
                .filter(rule -> AlarmRules.ENABLED_STATUS_ENABLE.equals(rule.getEnabledStatus()) && AlarmRules.POINT_TYPE_ACCUMULATE.equals(rule.getPointType()))
                .toList();
        if(CollectionUtils.isEmpty(rules)){
            return;
        }

        // 获取告警条件
        Map<Long,List<AlarmRulePoint>> rulePointMap = alarmRulePointService.getByAlarmRuleIds(rules.stream().map(AlarmRules::getId).toList())
                .stream()
                .filter(rulePoint -> rulePoint.getDeviceId() != null && rulePoint.getDeviceId().compareTo(deviceId) == 0)
                .collect(Collectors.groupingBy(AlarmRulePoint::getAlarmRuleId, Collectors.toList()));
        if(CollectionUtils.isEmpty(rulePointMap)){
            return;
        }
        // 获取规则列表
        Map<String,BigDecimal> values = new HashMap<>();
        for (AlarmRules rule : rules) {
            if (!rule.getEnabledStatus().equals(AlarmRules.ENABLED_STATUS_ENABLE)) {
                continue;
            }
            if (!AlarmRules.POINT_TYPE_ACCUMULATE.equals(rule.getPointType())) {
                continue;
            }
            List<AlarmRulePoint> rulePointList = rulePointMap.get(rule.getId());
            for (AlarmRulePoint rulePoint : rulePointList) {
                BigDecimal v = null;
                if(values.containsKey(rulePoint.getTimeGranularity())){
                    v = values.get(rulePoint.getTimeGranularity());
                }else {
                    // 获取值
                    switch (rulePoint.getTimeGranularity()) {
                        case "hour":
                            HourData hourData = hourDataService.findByDeviceIdAndTime(deviceId, hour);
                            v = hourData == null ? null : hourData.getValue();
                            break;
                        case "day":
                            DayData dayData = dayDataService.findByDeviceIdAndTime(deviceId,hour.toLocalDate().atStartOfDay());
                            v = dayData == null ? null : dayData.getValue();
                            break;
                        case "month":
                            MonthData monthData = monthDataService.findByDeviceIdAndTime(deviceId,hour.toLocalDate().withDayOfMonth(1).atStartOfDay());
                            v = monthData == null ? null : monthData.getValue();
                            break;
                        case "year":
                            YearData yearData = yearDataService.findByDeviceIdAndTime(deviceId,hour.toLocalDate().withDayOfYear(1).atStartOfDay());
                            v = yearData == null ? null : yearData.getValue();
                            break;
                    }
                    values.put(rulePoint.getTimeGranularity(),v);
                }

                if (v != null && operator(rulePoint, v)) {
                    createAlarmRecord(device, rule, rulePoint, v.toPlainString());
                }
            }
        }
    }

    /**
     * 计量规则点位告警检测
     *
     * @param meteringPointId 计量规则点位id
     * @param hour            小时
     */
    @Override
    public void alarmDetectionForMeteringPoint(Long meteringPointId, LocalDateTime hour) {
        MeteringPoint meteringPoint = meteringPointService.getById(meteringPointId);
        if(meteringPoint == null){
            return;
        }
        // 获取开启的报警条件
        List<AlarmRules> rules = alarmRulesService.list()
                .stream()
                .filter(rule -> AlarmRules.ENABLED_STATUS_ENABLE.equals(rule.getEnabledStatus()) && AlarmRules.POINT_TYPE_VIRTUAL.equals(rule.getPointType()))
                .toList();
        // 获取告警条件
        Map<Long,List<AlarmRulePoint>> alarmRulePointMap = alarmRulePointService.getByAlarmRuleIds(rules.stream().map(AlarmRules::getId).toList())
                .stream()
                .filter(rulePoint -> rulePoint.getDeviceId() != null && rulePoint.getDeviceId().compareTo(meteringPointId) == 0)
                .collect(Collectors.groupingBy(AlarmRulePoint::getAlarmRuleId, Collectors.toList()));
        Map<String,BigDecimal> values = new HashMap<>();
        for(AlarmRules rule : rules){
            if (!rule.getEnabledStatus().equals(AlarmRules.ENABLED_STATUS_ENABLE)) {
                continue;
            }
            if (!AlarmRules.POINT_TYPE_VIRTUAL.equals(rule.getPointType())) {
                continue;
            }
            List<AlarmRulePoint> rulePointList = alarmRulePointMap.get(rule.getId());
            if(rulePointList == null || rulePointList.isEmpty()){
                continue;
            }
            for (AlarmRulePoint rulePoint : rulePointList) {
                BigDecimal v = null;
                if(values.containsKey(rulePoint.getTimeGranularity())){
                    v = values.get(rulePoint.getTimeGranularity());
                }else {
                    // 获取值
                    switch (rulePoint.getTimeGranularity()) {
                        case "hour":
                            MeteringPointData hourData = meteringPointDataHourService.findByPointIdAndTime(meteringPointId,hour);
                            v = hourData == null ? null : hourData.getValue();
                            break;
                        case "day":
                            MeteringPointData dayData = meteringPointDataDayService.findByDateAndPointId(hour.toLocalDate(),meteringPointId);
                            v = dayData == null ? null : dayData.getValue();
                            break;
                        case "month":
                            MeteringPointData monthData = meteringPointDataMonthService.findByDateAndPointId(hour.toLocalDate().withDayOfMonth(1),meteringPointId);
                            v = monthData == null ? null : monthData.getValue();
                            break;
                        case "year":
                            MeteringPointData yearData = meteringPointDataYearService.findByDateAndPointId(hour.toLocalDate().withDayOfYear(1), meteringPointId);
                            v = yearData == null ? null : yearData.getValue();
                            break;
                    }
                    values.put(rulePoint.getTimeGranularity(),v);
                }

                if (v != null && operator(rulePoint, v)) {
                    createAlarmRecord(meteringPoint, rule, rulePoint, v.toPlainString());
                }
            }
        }
    }

    /**
     * 转事件工单
     *
     * @param data
     */
    @Override
    public void transferEvent(TransferEventDto data) {
        // 获取告警详情
        AlarmRecord record = super.getById(data.getRecordId());
        if(record == null){
            throw new JeecgBootException("告警记录不存在");
        }
        String alarmStatus = record.getAlarmStatus();
        if(!AlarmRecord.ALARM_STATUS_UNTREATED.equals(alarmStatus)){
            throw new JeecgBootException("状态已变更，请刷新页面");
        }
        BusinessDataDto businessData = new BusinessDataDto();
        businessData.setContractPeople(data.getContractPeople());
        businessData.setContractPhone(data.getContractPhone());
        businessData.setSpaceId(data.getSpaceId());
        businessData.setAddress(data.getAddress());
        businessData.setSpaceName(data.getSpaceName());
        businessData.setDescription(data.getDescription());
        String eventId = eventService.createEvent(businessData);
        super.update(new LambdaUpdateWrapper<AlarmRecord>()
                .eq(AlarmRecord::getId, data.getRecordId())
                .set(AlarmRecord::getEventId, eventId)
                .set(AlarmRecord::getAlarmStatus, AlarmRecord.ALARM_STATUS_EVENT)
        );
    }

    /**
     * 已处理
     *
     * @param eventId 事件id
     */
    @Override
    public void completed(String eventId) {
        super.update(new LambdaUpdateWrapper<AlarmRecord>()
                .eq(AlarmRecord::getEventId, eventId)
                .set(AlarmRecord::getAlarmStatus, AlarmRecord.ALARM_STATUS_TREATED)
        );
    }

    /**
     * TODO 生成告警记录
     *
     * @param device 设备信息
     * @param rule   告警规则
     * @param point  设备点位
     * @param value  值
     */
    private void createAlarmRecord(Device device, AlarmRules rule, AlarmRulePoint point, String value) {
        try {
            // 告警频率
            if (checkFrequency(rule, point)) {
                return;
            }
            String pointType = rule.getPointType();
            String content = "";
            switch (pointType) {
                case AlarmRules.POINT_TYPE_INSTANT:
                    // 瞬时值.{设备名称}{点位名称}{空间位置}{报警类型}，条件：{}，阈值：{}，异常值：{}
                    content = String.format("%s,%s,%s,%s，条件：%s，阈值：%s，异常值：%s",
                            device.getDeviceName(),
                            point.getPointName(),
                            device.getSpaceName(),
                            rule.getAlarmCategoryName(),
                            point.getOperator(),
                            point.getConditionValue(),
                            value);
                    break;
                case AlarmRules.POINT_TYPE_ACCUMULATE:
                    // 累计值.{设备名称}{空间位置}{时间粒度}{报警类型}，条件：{}，阈值：{}，异常值：{}
                    content = String.format("%s,%s,%s,%s，条件：%s，阈值：%s，异常值：%s",
                            point.getDeviceName(),
                            device.getSpaceName(),
                            point.getTimeGranularityStr(),
                            rule.getAlarmCategoryName(),
                            point.getOperator(),
                            point.getConditionValue(),
                            value);
                    break;
            }
            if (StringUtils.isEmpty(content)) {
                return;
            }
            AlarmRecord alarmRecord = new AlarmRecord();
            alarmRecord.setAlarmTime(LocalDateTime.now());
            alarmRecord.setAlarmCategoryId(rule.getAlarmCategoryId());
            alarmRecord.setAlarmCategoryName(rule.getAlarmCategoryName());
            alarmRecord.setAlarmLevelName(rule.getAlarmLevelName());
            alarmRecord.setAlarmRulePointId(point.getId());
            alarmRecord.setAlarmContent(content);
            alarmRecord.setAlarmLevelId(rule.getAlarmLevelId());
            alarmRecord.setAlarmRuleId(rule.getId());
            alarmRecord.setAlarmStatus(AlarmRecord.ALARM_STATUS_UNTREATED);
            alarmRecord.setDeviceId(device.getId());
            alarmRecord.setSpaceId(device.getSpaceId());
            alarmRecord.setSpaceName(device.getSpaceName());
            alarmRecord.setDeviceCategoryId(device.getCategoryId());
            alarmRecord.setDeviceName(device.getDeviceName());
            alarmRecord.setConditionValue(point.getConditionValue());
            alarmRecord.setValue(value);
            alarmRecord.setOperator(point.getOperator());
            alarmRecord.setPointId(point.getPointId());
            alarmRecord.setPointName(point.getPointName());
            alarmRecord.setTimeGranularity(point.getTimeGranularity());
            alarmRecord.setAlarmLevelColor(rule.getAlarmLevelColor());
            save(alarmRecord);
        } catch (Exception e) {
            log.error("告警记录生成失败，设备id：{},告警规则id：{},告警设备点位id：{},值：{}", device.getId(), rule.getId(), point.getId(), value, e);
        }
    }

    /**
     * 创建告警记录
     * @param meteringPoint 计量规则点位
     * @param rule 告警规则
     * @param point 点位配置
     * @param value 值
     */
    private void createAlarmRecord(MeteringPoint meteringPoint, AlarmRules rule, AlarmRulePoint point, String value) {
        try {
            // 告警频率
            if (checkFrequency(rule, point)) {
                return;
            }
            String pointType = rule.getPointType();

            if(!AlarmRules.POINT_TYPE_VIRTUAL.equals(pointType)){
                return;
            }
            // 累计值.{点位名称}{时间粒度}{报警类型}，条件：{}，阈值：{}，异常值：{}
            String content = meteringPoint.getNodeName() + point.getTimeGranularityStr() + rule.getAlarmCategoryName() + "，条件：" + point.getOperator() + "，阈值：" + point.getConditionValue() + "，异常值：" + value;
            AlarmRecord alarmRecord = new AlarmRecord();
            alarmRecord.setAlarmTime(LocalDateTime.now());
            alarmRecord.setAlarmCategoryId(rule.getAlarmCategoryId());
            alarmRecord.setAlarmCategoryName(rule.getAlarmCategoryName());
            alarmRecord.setAlarmLevelName(rule.getAlarmLevelName());
            alarmRecord.setAlarmRulePointId(point.getId());
            alarmRecord.setAlarmContent(content);
            alarmRecord.setAlarmLevelId(rule.getAlarmLevelId());
            alarmRecord.setAlarmRuleId(rule.getId());
            alarmRecord.setAlarmStatus(AlarmRecord.ALARM_STATUS_UNTREATED);
            alarmRecord.setDeviceId(meteringPoint.getId());
            alarmRecord.setSpaceId(meteringPoint.getSpaceId());
//            alarmRecord.setSpaceName(meteringPoint.getSpaceName());
            alarmRecord.setDeviceCategoryId(meteringPoint.getCategoryId());
            alarmRecord.setDeviceName(meteringPoint.getNodeName());
            alarmRecord.setConditionValue(point.getConditionValue());
            alarmRecord.setValue(value);
            alarmRecord.setOperator(point.getOperator());
            alarmRecord.setPointId(point.getPointId());
            alarmRecord.setPointName(point.getPointName());
            alarmRecord.setTimeGranularity(point.getTimeGranularity());
            alarmRecord.setAlarmLevelColor(rule.getAlarmLevelColor());
            save(alarmRecord);
        } catch (Exception e) {
            log.error("告警记录生成失败，计量规则点位id：{},告警规则id：{},告警设备点位id：{},值：{}", meteringPoint.getId(), rule.getId(), point.getId(), value, e);
        }
    }
    /**
     * 获取满足条件的告警规则id
     *
     * @param points    设备点位信息
     * @param pointType 点位类型
     * @param value     值
     */
    private void detection(Long deviceId, List<AlarmRulePoint> points, String pointType, String value) {
        if (CollectionUtils.isEmpty(points)) {
            return;
        }
        Map<Long, List<AlarmRulePoint>> rulePointMap = points.stream().collect(Collectors.groupingBy(AlarmRulePoint::getAlarmRuleId));
        // 获取规则列表
        List<AlarmRules> rules = alarmRulesService.listEnabledByIds(rulePointMap.keySet());
        if(CollectionUtils.isEmpty(rules)){
            return;
        }
        Device device = deviceService.getDetail(deviceId);
        if (device == null) {
            return;
        }
        BigDecimal v = new BigDecimal(value);

        for (AlarmRules rule : rules) {
            if (!pointType.equals(rule.getPointType())) {
                continue;
            }
            List<AlarmRulePoint> rulePointList = rulePointMap.get(rule.getId());
            for (AlarmRulePoint rulePoint : rulePointList) {
                if (operator(rulePoint, v)) {
                    createAlarmRecord(device, rule, rulePoint, value);
                }
            }
        }
    }

    private static boolean operator(AlarmRulePoint rulePoint, BigDecimal v) {
        BigDecimal conditionValue = new BigDecimal(rulePoint.getConditionValue());
        BigDecimal subtract = v.subtract(conditionValue);
        switch (rulePoint.getOperator()) {
            case ">":
                if (subtract.compareTo(BigDecimal.ZERO) > 0) {
                    return true;
                }
                break;
            case "=":
                if (subtract.compareTo(BigDecimal.ZERO) == 0) {
                    return true;
                }
                break;
            case "<":
                if (subtract.compareTo(BigDecimal.ZERO) < 0) {
                    return true;
                }
                break;
            case ">=":
                if (subtract.compareTo(BigDecimal.ZERO) >= 0) {
                    return true;
                }
                break;
            case "<=":
                if (subtract.compareTo(BigDecimal.ZERO) <= 0) {
                    return true;
                }
                break;
            case "!=":
                if (subtract.compareTo(BigDecimal.ZERO) != 0) {
                    return true;
                }
                break;
        }
        return false;
    }

    private LambdaQueryWrapper<AlarmRecord> getQueryWrapper(AlarmRecordDto params) {
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<AlarmRecord>()
                .eq(params.getSpaceId() != null, AlarmRecord::getSpaceId, params.getSpaceId())
                .eq(params.getAlarmLevelId() != null, AlarmRecord::getAlarmLevelId, params.getAlarmLevelId())
                .eq(params.getAlarmCategoryId() != null, AlarmRecord::getAlarmCategoryId, params.getAlarmCategoryId())
                .eq(StringUtils.isNotEmpty(params.getAlarmStatus()), AlarmRecord::getAlarmStatus, params.getAlarmStatus());
        if (StringUtils.isNotEmpty(params.getDeviceIds())) {
            wrapper.in(AlarmRecord::getDeviceId, Arrays.stream(params.getDeviceIds().split(",")).map(Long::parseLong).collect(Collectors.toList()));
        }
        if (params.getStartDateTime() != null && params.getEndDateTime() != null) {
            wrapper.between(AlarmRecord::getAlarmTime, params.getStartDateTime(), params.getEndDateTime());
        }
        return wrapper;
    }

    private boolean checkFrequency(AlarmRules rule, AlarmRulePoint point) {
        Integer frequency = rule.getFrequency();
        String frequencyUnit = rule.getFrequencyUnit();
        if (frequency == null || frequencyUnit == null) {
            return false;
        }
        LocalDateTime time = LocalDateTime.now();
        switch (frequencyUnit) {
            case "s":
                time = time.minusSeconds(frequency);
                break;
            case "m":
                time = time.minusMinutes(frequency);
                break;
            case "h":
                time = time.minusHours(frequency);
                break;
            case "d":
                time = time.minusDays(frequency);
                break;
            default:
                return true;
        }
        // 获取该设备该点位该时间段内是否已经生成过告警记录
        return count(new LambdaQueryWrapper<AlarmRecord>().eq(AlarmRecord::getAlarmRulePointId,point.getId()).gt(AlarmRecord::getAlarmTime, time)) > 0L;
    }
}
