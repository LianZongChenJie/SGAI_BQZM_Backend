package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataMonth;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IMeteringPointDataMonthService extends IService<MeteringPointDataMonth> {
    List<MeteringPointDataMonth> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds);
    void save(Long pointId, LocalDateTime time, BigDecimal value);
    List<MeteringPointDataMonth> findByDateAndPointIds(LocalDate date, List<Long> pointIds);
    MeteringPointDataMonth findByDateAndPointId(LocalDate date, Long pointId);
    List<MeteringPointDataMonth> findByTimeRangeAndPointId(LocalDate startDate, LocalDate endDate, Long pointId);
}
