package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointCostDataDay;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointCostDataMonth;
import org.jeecg.modules.bems.energyAnalysis.mapper.MeteringPointCostDataMonthMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointCostDataMonthService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class MeteringPointCostDataMonthServiceImpl extends ServiceImpl<MeteringPointCostDataMonthMapper, MeteringPointCostDataMonth> implements IMeteringPointCostDataMonthService {
    @Override
    public List<MeteringPointCostDataMonth> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds) {
        if(CollectionUtils.isEmpty(pointIds) || startTime == null || endTime == null){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<MeteringPointCostDataMonth>().in(MeteringPointCostDataMonth::getMeteringPointId, pointIds).between(MeteringPointCostDataMonth::getTime, startTime, endTime));
    }

    @Override
    public MeteringPointCostDataMonth findByTimeAndPointId(LocalDateTime time, Long pointId) {
        if(time == null || pointId == null){
            return null;
        }
        return getOne(new LambdaQueryWrapper<MeteringPointCostDataMonth>().eq(MeteringPointCostDataMonth::getMeteringPointId, pointId).eq(MeteringPointCostDataMonth::getTime, time));
    }

    @Override
    public List<MeteringPointCostDataMonth> findByTimeAndPointIds(LocalDateTime time, List<Long> pointIds) {
        if(time == null || CollectionUtils.isEmpty(pointIds)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<MeteringPointCostDataMonth>()
                .eq(MeteringPointCostDataMonth::getTime, time)
                .in(MeteringPointCostDataMonth::getMeteringPointId, pointIds));
    }
}
