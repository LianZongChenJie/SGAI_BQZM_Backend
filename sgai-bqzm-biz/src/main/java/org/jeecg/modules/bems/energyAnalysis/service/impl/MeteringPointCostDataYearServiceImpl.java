package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointCostDataYear;
import org.jeecg.modules.bems.energyAnalysis.mapper.MeteringPointCostDataYearMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointCostDataYearService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class MeteringPointCostDataYearServiceImpl extends ServiceImpl<MeteringPointCostDataYearMapper, MeteringPointCostDataYear> implements IMeteringPointCostDataYearService {
    @Override
    public MeteringPointCostDataYear findByTimeAndPointId(LocalDateTime time, Long pointId) {
        if(time == null || pointId == null){
            return null;
        }
        return getOne(new LambdaQueryWrapper<MeteringPointCostDataYear>().eq(MeteringPointCostDataYear::getMeteringPointId, pointId).eq(MeteringPointCostDataYear::getTime, time));
    }

    @Override
    public List<MeteringPointCostDataYear> findByTimeRangeAndPointIds(LocalDateTime startDate, LocalDateTime endDate, List<Long> pointIds) {
        if(pointIds == null || pointIds.isEmpty() || startDate == null || endDate == null){
            return Collections.emptyList();
        }
        return super.list(new LambdaQueryWrapper<MeteringPointCostDataYear>()
                .in(MeteringPointCostDataYear::getMeteringPointId, pointIds)
                .between(MeteringPointCostDataYear::getTime, startDate, endDate)
        );
    }
}
