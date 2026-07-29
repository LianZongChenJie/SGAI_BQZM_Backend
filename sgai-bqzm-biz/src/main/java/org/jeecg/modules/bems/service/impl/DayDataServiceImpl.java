package org.jeecg.modules.bems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.entity.DayData;
import org.jeecg.modules.bems.mapper.DayDataMapper;
import org.jeecg.modules.bems.service.IDayDataService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DayDataServiceImpl extends ServiceImpl<DayDataMapper,DayData> implements IDayDataService {

    private final RedisUtil redisUtil;

    private static final String CACHE_KEY_PREFIX = "device_energy_data_day:";

    private static final long CACHE_TIME = 60L * 60L * 30L;

    @Override
    public List<DayData> findByTime(LocalDateTime day) {
        return list(new LambdaQueryWrapper<DayData>().eq(DayData::getTime,day));
    }

    @Override
    public DayData findByDeviceIdAndTime(Long deviceId, LocalDateTime time) {
        String cacheKey = getCacheKey(deviceId,time);
        DayData dayData = (DayData)redisUtil.get(cacheKey);
        if(dayData == null){
            dayData = getOne(new LambdaQueryWrapper<DayData>().eq(DayData::getDeviceId,deviceId).eq(DayData::getTime,time));
        }
        if(dayData != null){
            redisUtil.set(cacheKey,dayData,CACHE_TIME);
        }
        return dayData;
    }

    @Override
    public List<DayData> findByDeviceIdsAndTime(Collection<Long> deviceIds, LocalDateTime time) {
        if(CollectionUtils.isEmpty(deviceIds)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<DayData>().eq(DayData::getTime,time).in(DayData::getDeviceId,deviceIds));
    }

    @Override
    public void preGeneration(List<Long> deviceIds, LocalDate date) {
        Map<Long, DayData> dayDataMap = this.findByTime(date.atStartOfDay())
                .stream()
                .collect(Collectors.toMap(DayData::getDeviceId, Function.identity(), (old, now) -> old));
        List<DayData> list = new ArrayList<>();
        for(Long deviceId : deviceIds){
            DayData dayData = dayDataMap.get(deviceId);
            if(dayData == null){
                dayData = new DayData();
                dayData.setDeviceId(deviceId);
                dayData.setTime(date.atStartOfDay());
                list.add(dayData);
            }
        }
        super.saveBatch(list);
        list.addAll(dayDataMap.values());
        for(DayData dayData : list){
            redisUtil.set(getCacheKey(dayData.getDeviceId(),dayData.getTime()),dayData,60L * 60L * 24L);
        }
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
    public boolean saveOrUpdate(DayData entity) {
        boolean b = super.saveOrUpdate(entity);
        if(b){
            redisUtil.set(getCacheKey(entity.getDeviceId(),entity.getTime()), entity, CACHE_TIME);
        }
        return b;
    }

    @Override
    public List<DayData> findByDeviceIdsAndTimeRange(Collection<Long> deviceIds, LocalDateTime startTime, LocalDateTime endTime) {
        if (CollectionUtils.isEmpty(deviceIds)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<DayData>().between(DayData::getTime,startTime,endTime).in(DayData::getDeviceId,deviceIds));
    }
}
