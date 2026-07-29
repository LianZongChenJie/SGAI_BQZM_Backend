package org.jeecg.modules.bems.energyAnalysis.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataMonth;
import org.jeecg.modules.bems.energyAnalysis.mapper.MeteringPointDataMonthMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataMonthService;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointRelService;
import org.jeecg.modules.bems.entity.MonthData;
import org.jeecg.modules.bems.service.IMonthDataService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class MeteringPointDataMonthServiceImpl extends ServiceImpl<MeteringPointDataMonthMapper,MeteringPointDataMonth> implements IMeteringPointDataMonthService {

    private final RedisUtil redisUtil;

    private static final String CACHE_KEY_PREFIX = "metering_point_data_month:";

    private static final Long CACHE_TIME = 60L * 90L;

    private final IMeteringPointRelService meteringPointRelService;

    private final IMonthDataService monthDataService;

    private final MeteringPointDataCalculationService calculationService;


    @Override
    public List<MeteringPointDataMonth> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds) {
//        return list(new LambdaQueryWrapper<MeteringPointDataMonth>().in(MeteringPointDataMonth::getMeteringPointId,pointIds).between(MeteringPointDataMonth::getTime,startTime,endTime));
        if(CollectionUtil.isEmpty(pointIds) || startTime == null || endTime == null){
            return Collections.emptyList();
        }
        // 获取点位关联设备信息
        List<Long> deviceIds = meteringPointRelService.findDeviceIdByPointIds(pointIds);
        // 获取设备小时数据
        List<MonthData> dataList = monthDataService.findByDeviceIdsAndTimeRange(deviceIds, startTime, endTime);
        return calculationService.calculation(pointIds, deviceIds, dataList).stream().map(MeteringPointDataMonth::convert).toList();
    }
    @Override
    public void save(Long pointId, LocalDateTime time, BigDecimal value) {
        // 判断点位时间是否存在
        MeteringPointDataMonth dataMonth = (MeteringPointDataMonth) redisUtil.get(getCacheKey(pointId, time));
        if(dataMonth == null) {
            dataMonth = getOne(new LambdaQueryWrapper<MeteringPointDataMonth>()
                    .eq(MeteringPointDataMonth::getMeteringPointId, pointId)
                    .eq(MeteringPointDataMonth::getTime, time)
            );
        }
        if(dataMonth == null){
            dataMonth = new MeteringPointDataMonth();
            dataMonth.setMeteringPointId(pointId);
            dataMonth.setTime(time);
        }
        dataMonth.setValue(value);
        saveOrUpdate(dataMonth);
        redisUtil.set(getCacheKey(pointId, time), dataMonth,CACHE_TIME);
    }

    @Override
    public List<MeteringPointDataMonth> findByDateAndPointIds(LocalDate date, List<Long> pointIds) {
        if(date == null || CollectionUtil.isEmpty(pointIds)){
            return Collections.emptyList();
        }
//        return list(new LambdaQueryWrapper<MeteringPointDataMonth>().eq(MeteringPointDataMonth::getTime,date.withDayOfMonth(1).atStartOfDay()).in(MeteringPointDataMonth::getMeteringPointId,pointIds));
        return findByTimeRangeAndPointIds(date.withDayOfMonth(1).atStartOfDay(),date.withDayOfMonth(date.lengthOfMonth()).atStartOfDay(),pointIds);
    }

    @Override
    public MeteringPointDataMonth findByDateAndPointId(LocalDate date, Long pointId) {
        if (date == null || pointId == null) {
            return null;
        }
//        List<MeteringPointDataMonth> list = list(new LambdaQueryWrapper<MeteringPointDataMonth>().eq(MeteringPointDataMonth::getTime, date.withDayOfMonth(1).atStartOfDay()).eq(MeteringPointDataMonth::getMeteringPointId, pointId));
//        if (CollectionUtil.isEmpty(list)) {
//            return null;
//        }
//        return list.get(0);
        List<MeteringPointDataMonth> data = findByTimeRangeAndPointIds(date.withDayOfMonth(1).atStartOfDay(), date.withDayOfMonth(date.lengthOfMonth()).atStartOfDay(), Collections.singletonList(pointId));
        return CollectionUtil.isEmpty(data) ? null : data.get(0);
    }

    @Override
    public List<MeteringPointDataMonth> findByTimeRangeAndPointId(LocalDate startDate, LocalDate endDate, Long pointId) {
        if(startDate == null || endDate == null || pointId == null){
            return Collections.emptyList();
        }
//        return list(
//                new LambdaQueryWrapper<MeteringPointDataMonth>()
//                        .eq(MeteringPointDataMonth::getMeteringPointId,pointId)
//                        .between(MeteringPointDataMonth::getTime,startDate.withDayOfMonth(1).atStartOfDay(),endDate.withDayOfMonth(1).atStartOfDay())
//        );
        return findByTimeRangeAndPointIds(startDate.withDayOfMonth(1).atStartOfDay(),endDate.withDayOfMonth(1).atStartOfDay(), Collections.singletonList(pointId));
    }

    private String getCacheKey(Long pointId,LocalDateTime time){
        return CACHE_KEY_PREFIX + pointId + ":" + time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}
