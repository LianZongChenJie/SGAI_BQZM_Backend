package org.jeecg.modules.bems.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.entity.RealData;
import org.jeecg.modules.bems.mapper.RealDataMapper;
import org.jeecg.modules.bems.service.IRealDataService;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RealDataServiceImpl extends ServiceImpl<RealDataMapper, RealData> implements IRealDataService {

    private final RedisUtil redisUtil;

    private final RedisTemplate<String,Object> redisTemplate;

    /**
     * 缓存时间,单位：秒
     */
    private static final long CACHE_TIME = 60L * 60L;
    /**
     * 缓存key前缀
     */
    private static final String CACHE_KEY_PREFIX = "device_energy_data_real:";

    /**
     * 设备最新值缓存key
     */
    private static final String CACHE_KEY_PREFIX_MAX = "device_energy_data_real_max:";

    @Override
    public void save(Long deviceId, LocalDateTime time, BigDecimal value) {
        RealData realDataMax = findLatest(deviceId);
        RealData realData = null;
        if(realDataMax == null || !realDataMax.getTime().isBefore(time)){
            realData = findByDeviceIdAndTime(deviceId,time);
        }
        if(realData == null){
            realData = new RealData();
            realData.setDeviceId(deviceId);
            realData.setTime(time);
        }
        realData.setValue(value);
        saveOrUpdate(realData);

        if(realDataMax == null || !realDataMax.getTime().isAfter(time)){
            redisUtil.set(getCacheKeyMax(deviceId), realData);
        }
        redisUtil.set(getCacheKey(deviceId,time), realData, CACHE_TIME);
    }

    /**
     * 获取最新的一条数据
     * @param deviceId 设备id
     * @return 最新的一条数据
     */
    private RealData findLatest(Long deviceId){
        RealData realData = (RealData)redisUtil.get(getCacheKeyMax(deviceId));
        if(realData == null){
            realData = findLatestByDeviceId(deviceId);
            if(realData != null){
                redisUtil.set(getCacheKeyMax(deviceId), realData);
            }
        }
        return realData;
    }

    private RealData findLatestByDeviceId(Long deviceId){
        List<RealData> list = list(new LambdaQueryWrapper<RealData>()
                .eq(RealData::getDeviceId, deviceId)
                .orderByDesc(RealData::getTime)
                .comment("limit 1"));
        if(CollectionUtil.isEmpty(list)){
            return null;
        }
        return list.get(0);
    }

    @Override
    public List<RealData> findByTime(LocalDateTime time) {
        return list(new LambdaQueryWrapper<RealData>()
                .eq(RealData::getTime,time));
    }

    @Override
    public RealData findByDeviceIdAndTime(Long deviceId, LocalDateTime time) {
        RealData realData = (RealData)redisUtil.get(getCacheKey(deviceId,time));
        if(realData == null){
            realData = getOne(new LambdaQueryWrapper<RealData>().eq(RealData::getDeviceId,deviceId).eq(RealData::getTime,time));
        }
        if(realData != null){
            redisUtil.set(getCacheKey(deviceId,time), realData, CACHE_TIME);
        }

        return realData;
    }

    @Override
    public List<RealData> findFirstByTimeRangeDesc(LocalDateTime startTime,LocalDateTime endTime) {
        return baseMapper.findFirstByTimeRangeDesc(startTime,endTime);
    }

    @Override
    public List<RealData> findFirstByTimeRangeAsc(LocalDateTime startTime,LocalDateTime endTime) {
        return baseMapper.findFirstByTimeRangeAsc(startTime,endTime);
    }

    @Override
    public List<RealData> findFirstByLtTimeDesc(LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.findFirstByLtTimeDesc(startTime, endTime);
    }

    @Override
    public List<RealData> findByDeviceIdAndTimeRange(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        return list(new LambdaQueryWrapper<RealData>().eq(RealData::getDeviceId,deviceId).between(RealData::getTime,startTime,endTime));
    }

    @Override
    public List<RealData> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return super.list(new LambdaQueryWrapper<RealData>()
                        .between(RealData::getTime,startTime,endTime)
                );
    }

    @Override
    public void preGeneration(List<Long> deviceIds, LocalDate date) {
        Map<Long, Map<LocalDateTime,RealData>> realDataMap = this.findByTimeRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream()
                .collect(Collectors.groupingBy(RealData::getDeviceId,
                        Collectors.toMap(RealData::getTime, Function.identity(), (k1, k2) -> k2)
                ));
        for(Long deviceId : deviceIds){
            Map<LocalDateTime, RealData> dataMap = realDataMap.getOrDefault(deviceId, new HashMap<>());
            List<RealData> list = new ArrayList<>();
            for(int i = 0; i < 96; i++){
                LocalDateTime time = date.atStartOfDay().plusMinutes(i * 15);
                RealData realData = dataMap.get(time);
                if(realData == null){
                    realData = new RealData();
                    realData.setDeviceId(deviceId);
                    realData.setTime(time);
                    list.add(realData);
                }
            }
            super.saveBatch(list);
            list.addAll(dataMap.values());
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public Object execute(@NotNull RedisOperations operations) throws DataAccessException {
                    for(RealData realData : list){
                        operations.opsForValue().set(getCacheKey(realData.getDeviceId(),realData.getTime()),realData,60L * 60L * 48L,TimeUnit.SECONDS);
                    }
                    return null;
                }
            });
//            for (RealData realData : list){
//                // 设置缓存时间为24小时，较少第二天查询数据库次数
//                redisUtil.set(getCacheKey(realData.getDeviceId(),realData.getTime()), realData, 60L * 60L * 24L);
//            }
        }
    }

    private String getCacheKey(Long deviceId,LocalDateTime time){
        return CACHE_KEY_PREFIX + deviceId + ":" + time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 设备最新值缓存key
     * @param deviceId 设备id
     */
    private String getCacheKeyMax(Long deviceId){
        return CACHE_KEY_PREFIX_MAX + deviceId;
    }

}
