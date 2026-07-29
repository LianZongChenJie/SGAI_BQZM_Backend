package org.jeecg.modules.bems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.entity.MonthData;
import org.jeecg.modules.bems.mapper.MonthDataMapper;
import org.jeecg.modules.bems.service.IMonthDataService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class MonthDataServiceImpl extends ServiceImpl<MonthDataMapper,MonthData> implements IMonthDataService {

    private final RedisUtil redisUtil;

    private static final String CACHE_KEY_PREFIX = "device_energy_data_month:";

    private static final long CACHE_TIME = 60L * 90L;

    @Override
    public List<MonthData> findByTime(LocalDateTime month) {
        return list(new LambdaQueryWrapper<MonthData>().eq(MonthData::getTime,month));
    }

    @Override
    public MonthData findByDeviceIdAndTime(Long deviceId, LocalDateTime time) {
        String cacheKey = getCacheKey(deviceId, time);
        MonthData monthData = (MonthData)redisUtil.get(cacheKey);
        if(monthData == null){
            monthData = getOne(new LambdaQueryWrapper<MonthData>().eq(MonthData::getDeviceId,deviceId).eq(MonthData::getTime,time));
        }
        if(monthData != null){
            redisUtil.set(cacheKey,monthData,CACHE_TIME);
        }
        return monthData;
    }

    @Override
    public List<MonthData> findByDeviceIdsAndTime(Collection<Long> deviceIds, LocalDateTime time) {
        if(CollectionUtils.isEmpty(deviceIds)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<MonthData>().eq(MonthData::getTime,time).in(MonthData::getDeviceId,deviceIds));
    }

    private String getCacheKey(Long deviceId,LocalDateTime time){
        return CACHE_KEY_PREFIX + deviceId + ":" + time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * TableId 注解存在更新记录，否插入一条记录
     *
     * @param entity 实体对象
     * @return boolean
     */
    @Override
    public boolean saveOrUpdate(MonthData entity) {
        boolean b = super.saveOrUpdate(entity);
        if(b){
            redisUtil.set(getCacheKey(entity.getDeviceId(),entity.getTime()), entity, CACHE_TIME);
        }
        return b;
    }

    @Override
    public List<MonthData> findByDeviceIdsAndTimeRange(Collection<Long> deviceIds, LocalDateTime startTime, LocalDateTime endTime) {
        if(CollectionUtils.isEmpty(deviceIds)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<MonthData>().between(MonthData::getTime,startTime,endTime).in(MonthData::getDeviceId,deviceIds));
    }
}
