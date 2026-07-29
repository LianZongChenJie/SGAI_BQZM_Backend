package org.jeecg.modules.bems.energyAnalysis.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataYear;
import org.jeecg.modules.bems.energyAnalysis.mapper.MeteringPointDataYearMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataYearService;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointRelService;
import org.jeecg.modules.bems.entity.YearData;
import org.jeecg.modules.bems.service.IYearDataService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class MeteringPointDataYearServiceImpl extends ServiceImpl<MeteringPointDataYearMapper,MeteringPointDataYear> implements IMeteringPointDataYearService {

    private final RedisUtil redisUtil;

    private static final String CACHE_KEY_PREFIX = "metering_point_data_year:";
    private static final Long CACHE_TIME = 60L * 60L * 30L;

    private final IMeteringPointRelService meteringPointRelService;

    private final IYearDataService yearDataService;

    private final MeteringPointDataCalculationService calculationService;


    @Override
    public void save(Long pointId, LocalDateTime time, BigDecimal value) {
        // 判断点位时间是否存在
        MeteringPointDataYear dataYear = (MeteringPointDataYear) redisUtil.get(getCacheKey(pointId, time));
        if(dataYear == null) {
            dataYear = getOne(new LambdaQueryWrapper<MeteringPointDataYear>()
                    .eq(MeteringPointDataYear::getMeteringPointId, pointId)
                    .eq(MeteringPointDataYear::getTime, time)
            );
        }
        if(dataYear == null){
            dataYear = new MeteringPointDataYear();
            dataYear.setMeteringPointId(pointId);
            dataYear.setTime(time);
        }
        dataYear.setValue(value);
        saveOrUpdate(dataYear);
        redisUtil.set(getCacheKey(pointId, time), dataYear,CACHE_TIME);
    }

    @Override
    public List<MeteringPointDataYear> findByDateAndPointIds(LocalDate date, List<Long> pointIds) {
        if(date == null || CollectionUtil.isEmpty(pointIds)){
            return Collections.emptyList();
        }
//        return list(new LambdaQueryWrapper<MeteringPointDataYear>().eq(MeteringPointDataYear::getTime,date.withMonth(1).withDayOfMonth(1).atStartOfDay()).in(MeteringPointDataYear::getMeteringPointId,pointIds));
        return findByTimeRangeAndPointIds(date.withDayOfYear(1).atStartOfDay(),date.withDayOfYear(date.lengthOfYear()).atStartOfDay(),pointIds);
    }

    @Override
    public List<MeteringPointDataYear> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds) {
//        return list(new LambdaQueryWrapper<MeteringPointDataYear>().in(MeteringPointDataYear::getMeteringPointId,pointIds).between(MeteringPointDataYear::getTime,startTime,endTime));
        if(CollectionUtil.isEmpty(pointIds) || startTime == null || endTime == null){
            return Collections.emptyList();
        }
        // 获取点位关联设备信息
        List<Long> deviceIds = meteringPointRelService.findDeviceIdByPointIds(pointIds);
        // 获取设备小时数据
        List<YearData> dataList = yearDataService.findByDeviceIdsAndTimeRange(deviceIds, startTime, endTime);
        return calculationService.calculation(pointIds, deviceIds, dataList).stream().map(MeteringPointDataYear::convert).toList();
    }

    @Override
    public MeteringPointDataYear findByDateAndPointId(LocalDate date, Long pointId) {
        if(date == null || pointId == null){
            return null;
        }
//        List<MeteringPointDataYear> list = super.list(new LambdaQueryWrapper<MeteringPointDataYear>().eq(MeteringPointDataYear::getMeteringPointId,pointId).eq(MeteringPointDataYear::getTime,date.withMonth(1).withDayOfMonth(1).atStartOfDay()));
//        if(CollectionUtil.isEmpty(list)){
//            return null;
//        }
//        return list.get(0);
        List<MeteringPointDataYear> data = findByTimeRangeAndPointIds(date.withDayOfYear(1).atStartOfDay(), date.withDayOfYear(date.lengthOfYear()).atStartOfDay(), Collections.singletonList(pointId));
        return data.isEmpty() ? null : MeteringPointDataYear.convert(data.get(0));
    }

    private String getCacheKey(Long pointId,LocalDateTime time){
        return CACHE_KEY_PREFIX + pointId + ":" + time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

}
