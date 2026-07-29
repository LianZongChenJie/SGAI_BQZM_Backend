package org.jeecg.modules.bems.energyAnalysis.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataMinute;
import org.jeecg.modules.bems.energyAnalysis.mapper.MeteringPointDataMinuteMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataMinuteService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class MeteringPointDataMinuteServiceImpl extends ServiceImpl<MeteringPointDataMinuteMapper, MeteringPointDataMinute> implements IMeteringPointDataMinuteService {

    private final RedisUtil redisUtil;

    private static final String CACHE_KEY_PREFIX_MAX = "metering_point_data_minute_max:";

    @Override
    public void save(Long pointId, LocalDateTime time, BigDecimal value) {
        MeteringPointDataMinute latest = findLatest(pointId);
        MeteringPointDataMinute minuteData = null;
        if(latest == null || !latest.getTime().isBefore(time)){
            minuteData = getOne(new LambdaQueryWrapper<MeteringPointDataMinute>().eq(MeteringPointDataMinute::getMeteringPointId, pointId).eq(MeteringPointDataMinute::getTime, time));
        }
        if(minuteData == null){
            minuteData = new MeteringPointDataMinute();
            minuteData.setMeteringPointId(pointId);
            minuteData.setTime(time);
        }
        minuteData.setValue(value);
        super.saveOrUpdate(minuteData);
        if(latest == null || !latest.getTime().isAfter(time)){
            redisUtil.set(getCacheKeyMax(pointId), minuteData);
        }
    }

    @Override
    public List<MeteringPointDataMinute> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds) {
        if(CollectionUtil.isEmpty(pointIds)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<MeteringPointDataMinute>()
                .in(MeteringPointDataMinute::getMeteringPointId,pointIds)
                .between(MeteringPointDataMinute::getTime,startTime,endTime));
    }

    private MeteringPointDataMinute findLatest(Long pointId){
        MeteringPointDataMinute minuteData = (MeteringPointDataMinute)redisUtil.get(getCacheKeyMax(pointId));
        if(minuteData == null){
            minuteData = findLatestByPointId(pointId);
            if(minuteData != null){
                redisUtil.set(getCacheKeyMax(pointId),minuteData);
            }
        }
        return minuteData;
    }

    private MeteringPointDataMinute findLatestByPointId(Long pointId){
        List<MeteringPointDataMinute> list = list(new LambdaQueryWrapper<MeteringPointDataMinute>()
                .eq(MeteringPointDataMinute::getMeteringPointId, pointId)
                .orderByDesc(MeteringPointDataMinute::getTime)
                .last("limit 1")
        );
        if(CollectionUtil.isEmpty(list)){
            return null;
        }
        return list.get(0);
    }

    private String getCacheKeyMax(Long pointId){
        return CACHE_KEY_PREFIX_MAX + pointId;
    }
}
