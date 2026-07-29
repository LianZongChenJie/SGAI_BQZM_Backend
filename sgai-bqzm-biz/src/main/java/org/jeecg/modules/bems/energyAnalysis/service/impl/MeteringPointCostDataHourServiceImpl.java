package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointCostDataHour;
import org.jeecg.modules.bems.energyAnalysis.mapper.MeteringPointCostDataHourMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointCostDataHourService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MeteringPointCostDataHourServiceImpl extends ServiceImpl<MeteringPointCostDataHourMapper, MeteringPointCostDataHour> implements IMeteringPointCostDataHourService {
    @Override
    public List<MeteringPointCostDataHour> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds) {
        return list(new LambdaQueryWrapper<MeteringPointCostDataHour>()
                .in(MeteringPointCostDataHour::getMeteringPointId,pointIds)
                .between(MeteringPointCostDataHour::getTime,startTime,endTime));
    }

    @Override
    public MeteringPointCostDataHour findByTimeAndPointId(LocalDateTime time, Long pointId) {
        return getOne(new LambdaQueryWrapper<MeteringPointCostDataHour>().eq(MeteringPointCostDataHour::getMeteringPointId,pointId).eq(MeteringPointCostDataHour::getTime,time));
    }
}
