package org.jeecg.modules.bems.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.entity.MinuteData;
import org.jeecg.modules.bems.mapper.MinuteDataMapper;
import org.jeecg.modules.bems.service.IMinuteDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MinuteDataServiceImpl extends ServiceImpl<MinuteDataMapper, MinuteData> implements IMinuteDataService {

    private final RedisUtil redisUtil;

    /**
     * 缓存key前缀
     */
    private static final String CACHE_KEY_PREFIX = "device_energy_data_minute:";

    /**
     * 设备最新值缓存key
     */
    private static final String CACHE_KEY_PREFIX_MAX = "device_energy_data_minute_max:";
    /**
     * 缓存时间,单位：秒
     */
    private static final long CACHE_TIME = 60L * 60L;

    @Override
    public MinuteData findByDeviceAndTime(Long deviceId, LocalDateTime time) {
        String cacheKey = getCacheKey(deviceId,time);
        MinuteData minuteData = (MinuteData)redisUtil.get(cacheKey);
        if(minuteData == null){
            MinuteData latest = findLatest(deviceId);
            if(latest == null || !latest.getTime().isBefore(time)) {
                minuteData = getOne(new LambdaQueryWrapper<MinuteData>()
                        .eq(MinuteData::getDeviceId, deviceId)
                        .eq(MinuteData::getTime, time));
            }else {
                return null;
            }
        }

        if(minuteData != null){
            redisUtil.set(cacheKey, minuteData, 60 * 60);
        }
        return minuteData;
    }

    @Override
    public List<MinuteData> findByDeviceIdsAndTime(Collection<Long> deviceIds, LocalDateTime time) {
        if(CollectionUtil.isEmpty(deviceIds) || time == null){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<MinuteData>()
                .eq(MinuteData::getTime, time)
                .in(MinuteData::getDeviceId, deviceIds));
    }

    @Override
    public List<MinuteData> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return super.list(new LambdaQueryWrapper<MinuteData>().between(MinuteData::getTime,startTime,endTime));
    }

    @Override
    public boolean saveOrUpdate(MinuteData data){
        boolean b = super.saveOrUpdate(data);
        if(b){
            redisUtil.set(getCacheKey(data.getDeviceId(),data.getTime()), data, CACHE_TIME);
            MinuteData max = (MinuteData)redisUtil.get(getCacheKeyMax(data.getDeviceId()));
            if(max == null || !max.getTime().isAfter(data.getTime())){
                redisUtil.set(getCacheKeyMax(data.getDeviceId()), data);
            }
        }
        return b;
    }

    @Override
    public void preGeneration(List<Long> deviceIds, LocalDate date) {
        Map<Long, Map<LocalDateTime, MinuteData>> minuteDataMap = this.findByTimeRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream()
                .collect(Collectors.groupingBy(MinuteData::getDeviceId,
                        Collectors.toMap(MinuteData::getTime, Function.identity(), (k1, k2) -> k2)
                ));
        for(Long deviceId : deviceIds){
            Map<LocalDateTime, MinuteData> dataMap = minuteDataMap.getOrDefault(deviceId,new HashMap<>());
            List<MinuteData> list = new ArrayList<>();
            for(int i = 0; i < 96; i++){
                LocalDateTime time = date.atStartOfDay().plusMinutes(i * 15);
                MinuteData minuteData = dataMap.get(time);
                if(minuteData == null) {
                    minuteData = new MinuteData();
                    minuteData.setDeviceId(deviceId);
                    minuteData.setTime(time);
                    // 新增
                    list.add(minuteData);
                }
            }
            super.saveBatch(list);
            list.addAll(dataMap.values());
            for (MinuteData minuteData : list){
                redisUtil.set(getCacheKey(minuteData.getDeviceId(),minuteData.getTime()), minuteData, 60L * 60L * 24L);
            }
        }
    }

    private MinuteData findLatest(Long deviceId){
        MinuteData minuteData = (MinuteData)redisUtil.get(getCacheKeyMax(deviceId));
        if(minuteData == null){
            minuteData = findLatestByDeviceId(deviceId);
            if(minuteData == null){
                return null;
            }
            redisUtil.set(getCacheKeyMax(deviceId), minuteData);
        }
        return minuteData;
    }

    private MinuteData findLatestByDeviceId(Long deviceId){
        List<MinuteData> list = list(
                new LambdaQueryWrapper<MinuteData>().eq(MinuteData::getDeviceId, deviceId)
                        .orderByDesc(MinuteData::getTime)
                        .comment("limit 1")
        );
        if(CollectionUtil.isEmpty( list)){
            return null;
        }
        return list.get(0);
    }

    private String getCacheKey(Long deviceId,LocalDateTime time){
        return CACHE_KEY_PREFIX + deviceId + ":" + time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 设备最新值缓存key
     * @param deviceId 设备id
     * @return 设备最新值缓存key
     */
    private String getCacheKeyMax(Long deviceId){
        return CACHE_KEY_PREFIX_MAX + deviceId;
    }

}
