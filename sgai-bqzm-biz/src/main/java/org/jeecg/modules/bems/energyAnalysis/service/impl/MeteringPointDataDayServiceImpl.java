package org.jeecg.modules.bems.energyAnalysis.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataDay;
import org.jeecg.modules.bems.energyAnalysis.mapper.MeteringPointDataDayMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataDayService;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointRelService;
import org.jeecg.modules.bems.entity.DayData;
import org.jeecg.modules.bems.service.IDayDataService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class MeteringPointDataDayServiceImpl extends ServiceImpl<MeteringPointDataDayMapper,MeteringPointDataDay> implements IMeteringPointDataDayService {

    private final MeteringPointDataDayMapper mapper;

    private final RedisUtil redisUtil;

    private static final String CACHE_KEY_PREFIX = "metering_point_data_day:";

    private static final Long CACHE_TIME = 60L * 60L * 30L;

    private final IMeteringPointRelService meteringPointRelService;

    private final MeteringPointDataCalculationService calculationService;

    private final IDayDataService dayDataService;

    @Override
    public List<MeteringPointDataDay> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds) {
        if(CollectionUtil.isEmpty(pointIds) || startTime == null || endTime == null){
            return Collections.emptyList();
        }
//        return list(new LambdaQueryWrapper<MeteringPointDataDay>().in(MeteringPointDataDay::getMeteringPointId,pointIds).between(MeteringPointDataDay::getTime,startTime,endTime));
        // 获取点位关联设备信息
        List<Long> deviceIds = meteringPointRelService.findDeviceIdByPointIds(pointIds);
        // 获取设备小时数据
        List<DayData> dataList = dayDataService.findByDeviceIdsAndTimeRange(deviceIds, startTime, endTime);
        return calculationService.calculation(pointIds, deviceIds, dataList).stream().map(MeteringPointDataDay::convert).toList();

    }
    @Override
    public void save(Long pointId, LocalDateTime time, BigDecimal value) {
        // 判断点位时间是否存在
        MeteringPointDataDay dataDay = (MeteringPointDataDay) redisUtil.get(getCacheKey(pointId, time));
        if(dataDay == null) {
            dataDay = getOne(new LambdaQueryWrapper<MeteringPointDataDay>()
                    .eq(MeteringPointDataDay::getMeteringPointId, pointId)
                    .eq(MeteringPointDataDay::getTime, time)
            );
        }
        if(dataDay == null){
            dataDay = new MeteringPointDataDay();
            dataDay.setMeteringPointId(pointId);
            dataDay.setTime(time);
        }
        dataDay.setValue(value);
        saveOrUpdate(dataDay);
        redisUtil.set(getCacheKey(pointId, time), dataDay,CACHE_TIME);
    }

    @Override
    public List<MeteringPointDataDay> findByDateAndPointIds(LocalDate date, List<Long> pointIds) {
        if(date == null || CollectionUtil.isEmpty(pointIds)){
            return Collections.emptyList();
        }
//        return list(new LambdaQueryWrapper<MeteringPointDataDay>().eq(MeteringPointDataDay::getTime,date.atStartOfDay()).in(MeteringPointDataDay::getMeteringPointId,pointIds));
        return findByTimeRangeAndPointIds(date.atStartOfDay(),date.atTime(LocalTime.MAX),pointIds);
    }

    @Override
    public MeteringPointDataDay findByDateAndPointId(LocalDate date, Long pointId) {
        if(date == null || pointId == null){
            return null;
        }
//        List<MeteringPointDataDay> list = super.list(new LambdaQueryWrapper<MeteringPointDataDay>().eq(MeteringPointDataDay::getTime,date.atStartOfDay()).eq(MeteringPointDataDay::getMeteringPointId,pointId));
//        if(CollectionUtils.isEmpty(list)) {
//            return null;
//        }
//        return list.get(0);
        List<MeteringPointDataDay> dataList = findByTimeRangeAndPointIds(date.atStartOfDay(), date.atTime(LocalTime.MAX), Collections.singletonList(pointId));
        return CollectionUtil.isEmpty(dataList) ? null : dataList.get(0);
    }

    @Override
    public List<MeteringPointDataDay> findByTimeRangeAndPointId(LocalDate startDate, LocalDate endDate, Long pointId) {
        if(pointId == null || startDate == null || endDate == null){
            return Collections.emptyList();
        }
//        return list(new LambdaQueryWrapper<MeteringPointDataDay>().eq(MeteringPointDataDay::getMeteringPointId,pointId).between(MeteringPointDataDay::getTime,startDate.atStartOfDay(),endDate.atStartOfDay()));
        return findByTimeRangeAndPointIds(startDate.atStartOfDay(), endDate.atStartOfDay(), Collections.singletonList(pointId));
    }

    /**
     * 查询平均值，小于时间的数据
     *
     * @param date    时间
     * @param pointId 计量规则点位id
     */
    @Override
    public BigDecimal findAvgByLtTimeAndPointId(LocalDate date, Long pointId) {
        // TODO 节能项目使用，暂为实现
        return mapper.findAvgByLtTimeAndPointId(date.atStartOfDay(), pointId);
    }

    private String getCacheKey(Long pointId,LocalDateTime time){
        return CACHE_KEY_PREFIX + pointId + ":" + time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}
