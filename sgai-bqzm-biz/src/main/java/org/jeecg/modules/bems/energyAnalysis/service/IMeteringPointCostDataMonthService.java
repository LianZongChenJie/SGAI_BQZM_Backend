package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointCostDataMonth;

import java.time.LocalDateTime;
import java.util.List;

public interface IMeteringPointCostDataMonthService extends IService<MeteringPointCostDataMonth> {
    List<MeteringPointCostDataMonth> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds);

    MeteringPointCostDataMonth findByTimeAndPointId(LocalDateTime time, Long pointId);

    List<MeteringPointCostDataMonth> findByTimeAndPointIds(LocalDateTime time, List<Long> pointIds);
}
