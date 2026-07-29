package org.jeecg.modules.bems.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.constant.CommonConstant;
import org.jeecg.modules.bems.constant.LogConstant;
import org.jeecg.modules.bems.dto.DeviceDataFindDto;
import org.jeecg.modules.bems.entity.*;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mq.send.MqSendService;
import org.jeecg.modules.bems.service.*;
import org.jeecg.modules.bems.vo.DeviceDataVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@AllArgsConstructor
@Slf4j
public class DeviceDataServiceImpl implements IDeviceDataService {
    private final RedisUtil redisUtil;

    private final IDeviceDataAmendLogService deviceDataAmendLogService;
    private final IRealDataService realDataService;
    private final IDeviceService deviceService;
    private final IHourDataService hourDataService;
    private final IMinuteDataService minuteDataService;
    private final IDayDataService dayDataService;
    private final IMonthDataService monthDataService;
    private final IYearDataService yearDataService;

    private final MqSendService mqSendService;

    private final IDataAmendLogService dataAmendLogService;

    @Override
    public IPage<DeviceDataVo> findList(DeviceDataFindDto params) {
        IPage<DeviceDataVo> listPage = deviceService.find(params.convertToDevice()).convert(DeviceDataVo::convert);
        List<RealData> startData = realDataService.findFirstByTimeRangeAsc(params.getStartTime(), params.getEndTime());
        List<RealData> endData = realDataService.findFirstByTimeRangeDesc(params.getStartTime(), params.getEndTime());
        // 计算值
        supplementStartAndEndData(listPage.getRecords(),startData,endData);
        return listPage;
    }

    /**
     * 状态数据
     * 起始值：小于开始时间当天23:59:59的最后一个值
     * 结束值：小于结束时间最后一个值
     * 当起始时间或结束时间当天没数据时获取最近的一个值
     */
    @Override
    public IPage<DeviceDataVo> findList1(DeviceDataFindDto params){
        // 获取设备信息
        IPage<DeviceDataVo> listPage = deviceService.find(params.convertToDevice()).convert(DeviceDataVo::convert);
        // @TODO findFirstByLtTimeDesc 已增加 startTime 参数，需补充传入
        List<RealData> startData = realDataService.findFirstByLtTimeDesc(params.getStartTime().minusMonths(1), params.getStartTime().with(LocalTime.MAX));
        // @TODO findFirstByLtTimeDesc 已增加 startTime 参数，需补充传入
        List<RealData> endData = realDataService.findFirstByLtTimeDesc(params.getStartTime(), params.getEndTime());
        supplementStartAndEndData(listPage.getRecords(),startData,endData);
        if(StrUtil.isNotEmpty(params.getConvertInteger()) && "1".equals(params.getConvertInteger())){
            // 数据四舍五入,只要整数
            listPage.getRecords().forEach(deviceDataVo -> {
                deviceDataVo.setValue(deviceDataVo.getValue().setScale(0, RoundingMode.HALF_UP));
                deviceDataVo.setStartValue(deviceDataVo.getStartValue().setScale(0, RoundingMode.HALF_UP));
                deviceDataVo.setEndValue(deviceDataVo.getEndValue().setScale(0, RoundingMode.HALF_UP));
            });
        }
        return listPage;
    }

    @Override
    public IPage<DeviceDataVo> deviceStatusList(DeviceDataFindDto params) {
        return deviceService.find(params.convertToDevice()).convert(DeviceDataVo::convert);
    }

    /**
     * 计算设备能耗
     *
     * @param device 设备
     * @param time   时间
     * @param value  表底值
     */
    @Override
    @Transactional
    public void calculateValue(Device device, LocalDateTime time, BigDecimal value) {
        // 保存表底值
        realDataService.save(device.getId(), time, value);
        // 保存分钟能耗
        saveMinuteData(device, time, value);
        // TODO 根据时间来判断，是15分钟值还是小时值，后续不确定数据来源怎么取，暂定
        if (time.getMinute() != 0) {
            return;
        }

        // 获取上小时表底值
        RealData last = realDataService.findByDeviceIdAndTime(device.getId(), time.minusHours(1));
        // 计算值
        BigDecimal hourValue = last == null || last.getValue() == null || value == null ? null : value.subtract(last.getValue()).multiply(device.getMagnification());
        // 最终值
        BigDecimal finalValue = hourValue;
        if(hourValue == null || hourValue.compareTo(BigDecimal.ZERO) < 0){
            // 自动修正
            finalValue = autoAmendData(device,time,hourValue);
        }

        HourData hour = hourDataService.findByDeviceIdAndTime(device.getId(), time.minusHours(1));
        if (hour == null) {
            hour = new HourData();
            hour.setDeviceId(device.getId());
            hour.setTime(time.minusHours(1));
            hour.setValue(BigDecimal.ZERO);
        }
        BigDecimal updValue = finalValue == null ? finalValue : finalValue.subtract(hour.getValue());
        hour.setStartValue(last == null || last.getValue() == null ? null : last.getValue().multiply(device.getMagnification()));
        hour.setEndValue(value == null ? null : value.multiply(device.getMagnification()));
        // 设置计算值
        hour.setComputeValue(hourValue);
        // 设置最终值
        hour.setValue(finalValue);
        hourDataService.saveOrUpdate(hour);
        // 更新能耗数据
        if(updValue != null) {
            saveEnergyConsumption(device.getId(), time.minusHours(1), updValue);
        }
        try {
            // hourValue不等于finalValue
            if (updValue != null && updValue.compareTo(BigDecimal.ZERO) != 0 && ((hourValue == null && finalValue != null) || (hourValue != null && finalValue != null && hourValue.compareTo(finalValue) != 0))) {
                // 保存修正日志
                dataAmendLogService.save(
                        new DataAmendLog(null,
                                hour.getDeviceId(),
                                hour.getId(),
                                hour.getTime(),
                                hour.getStartValue(),
                                hour.getEndValue(),
                                hour.getComputeValue(),
                                hour.getComputeValue(),
                                hour.getValue(),
                                "系统修正",
                                new Date()
                                ));
            }
        }catch (Exception e){
            log.error("保存修正日志错误：设备id: {},时间：{}",device.getId(),time.minusHours(1), e);
        }
        try {
            mqSendService.sendEnergyConsumptionChange(device.getId(), time.minusHours(1));
        }catch (Exception e){
            log.error("发送能耗数据变更错误：设备id: {},时间：{}",device.getId(),time.minusHours(1), e);
        }
    }

    /**
     * 计算设备能耗
     *
     * @param deviceCode 设备编码
     * @param time       时间
     * @param value      表底值
     */
    @Override
    public void calculateValue(String deviceCode, LocalDateTime time, BigDecimal value) {
        // 获取设备信息
        Device device = deviceService.getByDeviceCode(deviceCode);
        if(device == null){
            return;
        }
        if(device.getMagnification() == null){
            return;
        }
        calculateValue(device, time, value);
    }

    /**
     * 计算设备能耗-日
     *
     * @param deviceCode 设备编码
     * @param time       时间
     * @param value      表底值
     */
    @Override
    @Transactional
    public void calculateValueDay(String deviceCode, LocalDateTime time, BigDecimal value) {
         // 获取设备信息
        Device device = deviceService.getByDeviceCode(deviceCode);
        if(device == null){
            return;
        }
        // 校验是否重复
        RealData oldRealData = realDataService.findByDeviceIdAndTime(device.getId(), time);
        if(oldRealData != null && oldRealData.getValue().compareTo(value) == 0){
            return;
        }
        // 保存表底值
        realDataService.save(device.getId(), time, value);
        LocalDateTime day = time.toLocalDate().atStartOfDay();
        // 获取昨日表底数据
        Optional<RealData> min = realDataService.findByDeviceIdAndTimeRange(device.getId(), day.minusDays(1), day.minusSeconds(1))
                .stream().min(Comparator.comparing(RealData::getTime).reversed());
        RealData lastDayRelData = min.orElse(null);
        if(lastDayRelData == null){
            return;
        }
        // 获取当天能耗数据
        DayData dayData = dayDataService.findByDeviceIdAndTime(device.getId(), day);
        if(dayData == null){
            dayData = new DayData();
            dayData.setDeviceId(device.getId());
            dayData.setTime(day);
            dayData.setValue(BigDecimal.ZERO);
        }
        BigDecimal dayValue = value.subtract(lastDayRelData.getValue());
        BigDecimal updValue = dayValue.subtract(dayData.getValue());
        dayData.setValue(dayValue);
        if(updValue.compareTo(BigDecimal.ZERO) == 0){
            return;
        }

        MonthData monthData = monthDataService.findByDeviceIdAndTime(device.getId(), day.withDayOfMonth(1));
        if(monthData == null){
            monthData = new MonthData();
            monthData.setDeviceId(device.getId());
            monthData.setTime(day.withDayOfMonth(1));
            monthData.setValue(BigDecimal.ZERO);
        }
        monthData.setValue(monthData.getValue().add(updValue));

        YearData yearData = yearDataService.findByDeviceIdAndTime(device.getId(), day.withDayOfYear(1));
        if(yearData == null){
            yearData = new YearData();
            yearData.setDeviceId(device.getId());
            yearData.setTime(day.withDayOfYear(1));
            yearData.setValue(BigDecimal.ZERO);
        }
        yearData.setValue(yearData.getValue().add(updValue));

        dayDataService.saveOrUpdate(dayData);
        monthDataService.saveOrUpdate(monthData);
        yearDataService.saveOrUpdate(yearData);
        mqSendService.sendEnergyConsumptionChange(device.getId(), day);
    }

    public BigDecimal autoAmendData(Device device, LocalDateTime time, BigDecimal hourValue){
        BigDecimal finalValue = null;
        if(!"1".equals(device.getAutomaticAlgorithm())){
            // 禁用自动算法
            return null;
        }
        if (hourValue == null) {
            String logContent = "自动修正：获取表底值失败。%s。时间：" + time.format(CommonConstant.DATE_TIME_FORMATTER) + "；设备名称：" + device.getDeviceName() + ";设备编号：" + device.getDeviceCode();
            // 异常数据 获取前三个小时小时能耗，按照比例设置值
            List<BigDecimal> hourValues = hourDataService.findByDeviceIdAndTimes(device.getId(), IntStream.rangeClosed(3, 5).mapToObj(time::minusHours).collect(Collectors.toList()))
                    .stream().sorted(Comparator.comparing(HourData::getTime)).map(HourData::getValue).collect(Collectors.toList());
            if (CollectionUtil.isEmpty(hourValues) || hourValues.size() != 3) {
                // 保存日志 前三小时数据不足
                deviceDataAmendLogService.saveAmendLog(String.format(logContent, "修正失败，前三小时数据不足"),LogConstant.OPERATE_TYPE_101);
                return null;
            }
            // TODO 获取比例配置 5:3:2
            String scaleConfig = "5:3:2";
            String[] split = scaleConfig.split(":");
            if (split.length != 3) {
                // 比例配置错误
                deviceDataAmendLogService.saveAmendLog(String.format(logContent, "修正失败，比例配置错误"), LogConstant.OPERATE_TYPE_101);
                return null;
            }

            finalValue = hourValues.get(2).multiply(new BigDecimal(split[0]))
                    .add(hourValues.get(1).multiply(new BigDecimal(split[1])))
                    .add(hourValues.get(0).multiply(new BigDecimal(split[2])))
                    .divide(new BigDecimal(10), RoundingMode.HALF_UP);

            deviceDataAmendLogService.saveAmendLog(String.format(logContent, "修正成功,修正前：" + hourValue + ";修正后：" + finalValue), LogConstant.OPERATE_TYPE_101);
        }else if (hourValue.compareTo(BigDecimal.ZERO) < 0) {
            // 异常数据 获取上小时表地址
            List<BigDecimal> realList = realDataService.findByDeviceIdAndTimeRange(device.getId(), time.minusHours(1), time)
                    .stream().sorted(Comparator.comparing(RealData::getTime)).map(RealData::getValue).collect(Collectors.toList());
            finalValue = Collections.max(realList).subtract(realList.get(0))
                    .add(realList.get(realList.size() - 1)).subtract(Collections.min(realList));
            deviceDataAmendLogService.saveAmendLog("自动修正：计算值为负。修正前：" + hourValue + ";修正后：" + finalValue + "时间：" + time.format(CommonConstant.DATE_TIME_FORMATTER) + "；设备名称：" + device.getDeviceName() + ";设备编号：" + device.getDeviceCode(),LogConstant.OPERATE_TYPE_101);
        }
        return finalValue;
    }

    /**
     * 能耗数据修正
     *  修改小时数据值，同步修改日数据值，同步修改月数据值，同步修改年数据值
     * @param id 小时数据id，data_hour 主键
     * @param value 最终值
     */
    @Override
    @Transactional
    public void dataAmend(Long id, BigDecimal value) {
        if(value == null){
            return;
        }
        // 获取原始数据值
        HourData hour = hourDataService.getById(id);
        if(hour == null){
            return;
        }
        if(hour.getValue() != null && hour.getValue().compareTo(value) == 0){
            return;
        }
        DataAmendLog dataAmendLog = new DataAmendLog();
        dataAmendLog.setOriginalValue(hour.getValue());
        dataAmendLog.setValue(value);
        BigDecimal oldValue = hour.getValue() == null ? BigDecimal.ZERO : hour.getValue();
        hour.setValue(value);
        Device device = deviceService.getById(hour.getDeviceId());
        if(device == null){
            device = new Device();
        }
        // 保存日志
        hourDataService.updateById(hour);
        // 更新日、月、年数据
        saveEnergyConsumption(hour.getDeviceId(), hour.getTime(), value.subtract(oldValue));
        // 保存修正记录
        try{
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            dataAmendLog.setDeviceId(hour.getDeviceId());
            dataAmendLog.setHourDataId(hour.getId());
            dataAmendLog.setTime(hour.getTime());
            dataAmendLog.setStartValue(hour.getStartValue());
            dataAmendLog.setEndValue(hour.getEndValue());
            dataAmendLog.setComputeValue(hour.getComputeValue());
            dataAmendLog.setUpdateBy(sysUser.getUsername());
            dataAmendLog.setUpdateTime(new Date());
            dataAmendLogService.save(dataAmendLog);
        }catch (Exception e){
            log.error("保存修正日志错误：设备id: {},时间：{}",device.getId(),hour.getTime(), e);
        }
        // TODO 校正日志存储
        deviceDataAmendLogService.saveAmendLog("手动修正：修正成功。时间：" + hour.getTime().format(CommonConstant.DATE_TIME_FORMATTER) + "；设备名称：" + device.getDeviceName() + ";设备编号：" + device.getDeviceCode() + "；原值：" + oldValue + "；修正值：" + value,LogConstant.OPERATE_TYPE_102);
        try{
            mqSendService.sendEnergyConsumptionChange(device.getId(), hour.getTime());
        }catch (Exception e){
            log.error("发送能耗数据变更错误：设备id: {},时间：{}",device.getId(),hour.getTime(), e);
        }
    }

    /**
     * 保存能耗数据
     *
     * @param deviceId   设备id
     * @param time     时间
     * @param updValue 计算值
     */
    private void saveEnergyConsumption(Long deviceId, LocalDateTime time, BigDecimal updValue) {
        DayData day = dayDataService.findByDeviceIdAndTime(deviceId, time.withHour(0));
        if (day == null) {
            day = new DayData();
            day.setDeviceId(deviceId);
            day.setTime(time.withHour(0));
            day.setValue(BigDecimal.ZERO);
        }
        day.setValue(day.getValue().add(updValue));
        MonthData month = monthDataService.findByDeviceIdAndTime(deviceId, time.withDayOfMonth(1).withHour(0));
        if (month == null) {
            month = new MonthData();
            month.setDeviceId(deviceId);
            month.setTime(time.withDayOfMonth(1).withHour(0));
            month.setValue(BigDecimal.ZERO);
        }
        month.setValue(month.getValue().add(updValue));
        YearData year = yearDataService.findByDeviceIdAndTime(deviceId, time.withDayOfMonth(1).withMonth(1).withHour(0));
        if (year == null) {
            year = new YearData();
            year.setDeviceId(deviceId);
            year.setTime(time.withDayOfMonth(1).withMonth(1).withHour(0));
            year.setValue(BigDecimal.ZERO);
        }
        year.setValue(year.getValue().add(updValue));
        dayDataService.saveOrUpdate(day);
        monthDataService.saveOrUpdate(month);
        yearDataService.saveOrUpdate(year);
    }

    private void saveMinuteData(Device device, LocalDateTime time, BigDecimal value) {
        if(value != null) {
            value = value.multiply(device.getMagnification());
        }
        // 计算15分钟能耗
        MinuteData now = minuteDataService.findByDeviceAndTime(device.getId(), time.minusMinutes(15));
        MinuteData lastData = minuteDataService.findByDeviceAndTime(device.getId(), time.minusMinutes(30));
        if (now == null) {
            now = new MinuteData();
            now.setDeviceId(device.getId());
            now.setTime(time.minusMinutes(15));
        }
        now.setEndValue(value);
        if (lastData != null && lastData.getEndValue() != null) {
            now.setStartValue(lastData.getEndValue());
            if(now.getEndValue() != null){
                now.setValue(now.getEndValue().subtract(now.getStartValue()));
            }
        }
        minuteDataService.saveOrUpdate(now);
        // 往缓存里塞一份
//        redisUtil.set(RedisConstant.MINUTE_DATA_KEY + "::" + time.format(RedisConstant.TIME_FORMATTER) + "_" + device.getId(), now, RedisConstant.MINUTE_DATE_TTL);
    }


    private void supplementStartAndEndData(List<DeviceDataVo> list,List<RealData> startData,List<RealData> endData){
        for (DeviceDataVo deviceDataVo : list) {
            // 获取开始时间
            RealData start = startData.stream().filter(realData -> realData.getDeviceId().equals(deviceDataVo.getDeviceId())).findFirst().orElse(null);
            // 获取结束时间
            RealData end = endData.stream().filter(realData -> realData.getDeviceId().equals(deviceDataVo.getDeviceId())).findFirst().orElse(null);
            // 计算值
            if(deviceDataVo.getStartTime() == null){
                deviceDataVo.setStartTime(start == null ? null : start.getTime());
                deviceDataVo.setStartValue(start == null ? BigDecimal.ZERO : start.getValue().setScale(2, RoundingMode.HALF_UP));
            }
            if(deviceDataVo.getEndTime() == null){
                deviceDataVo.setEndTime(end == null ? null : end.getTime());
                deviceDataVo.setEndValue(end == null ? BigDecimal.ZERO : end.getValue().setScale(2, RoundingMode.HALF_UP));
            }
            deviceDataVo.setValue(end == null ? BigDecimal.ZERO : deviceDataVo.getEndValue().subtract(deviceDataVo.getStartValue()));
        }
    }

}
