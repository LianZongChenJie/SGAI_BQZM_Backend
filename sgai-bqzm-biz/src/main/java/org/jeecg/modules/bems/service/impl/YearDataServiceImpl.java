package org.jeecg.modules.bems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.entity.YearData;
import org.jeecg.modules.bems.mapper.YearDataMapper;
import org.jeecg.modules.bems.service.IYearDataService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class YearDataServiceImpl extends ServiceImpl<YearDataMapper,YearData> implements IYearDataService {

    private final RedisUtil redisUtil;

    private static final String CACHE_KEY_PREFIX = "device_energy_data_year:";

    private static final long CACHE_TIME = 60L * 90L;

    @Override
    public List<YearData> findByTime(LocalDateTime year) {
        return list(new LambdaQueryWrapper<YearData>().eq(YearData::getTime,year));
    }

    @Override
    public YearData findByDeviceIdAndTime(Long deviceId, LocalDateTime time) {
        String cacheKey = getCacheKey(deviceId, time);
        YearData yearData = (YearData)redisUtil.get(cacheKey);
        if(yearData == null){
            yearData = getOne(new LambdaQueryWrapper<YearData>().eq(YearData::getDeviceId,deviceId).eq(YearData::getTime,time));
        }
        if(yearData != null){
            redisUtil.set(cacheKey,yearData,CACHE_TIME);
        }
        return yearData;
    }

    @Override
    public boolean saveOrUpdate(YearData entity){
        boolean b = super.saveOrUpdate(entity);
        if(b){
            redisUtil.set(getCacheKey(entity.getDeviceId(),entity.getTime()), entity, CACHE_TIME);
        }
        return b;
    }

    @Override
    public List<YearData> findByDeviceIdsAndTimeRange(Collection<Long> deviceIds, LocalDateTime startTime, LocalDateTime endTime) {
        if(CollectionUtils.isEmpty(deviceIds)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<YearData>().between(YearData::getTime,startTime,endTime).in(YearData::getDeviceId,deviceIds));

    }

    @Override
    public List<YearData> findByDeviceIdsAndTime(Collection<Long> deviceIds, LocalDateTime time) {
        if(CollectionUtils.isEmpty(deviceIds)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<YearData>().eq(YearData::getTime,time).in(YearData::getDeviceId,deviceIds));
    }

    private String getCacheKey(Long deviceId, LocalDateTime time){
        return CACHE_KEY_PREFIX + deviceId + ":" + time.getYear();
    }
}
