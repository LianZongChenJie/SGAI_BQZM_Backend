package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointCostDataDay;
import org.jeecg.modules.bems.energyAnalysis.mapper.MeteringPointCostDataDayMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointCostDataDayService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MeteringPointCostDataDayServiceImpl extends ServiceImpl<MeteringPointCostDataDayMapper, MeteringPointCostDataDay> implements IMeteringPointCostDataDayService {
    @Override
    public List<MeteringPointCostDataDay> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds) {
        return list(new LambdaQueryWrapper<MeteringPointCostDataDay>()
                .between(MeteringPointCostDataDay::getTime,startTime,endTime)
                .in(MeteringPointCostDataDay::getMeteringPointId,pointIds)
        );
    }

    @Override
    public MeteringPointCostDataDay findByTimeAndPointId(LocalDateTime time, Long pointId) {
        return getOne(new LambdaQueryWrapper<MeteringPointCostDataDay>().eq(MeteringPointCostDataDay::getMeteringPointId, pointId).eq(MeteringPointCostDataDay::getTime, time));
    }
}
