package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataYear;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IMeteringPointDataYearService extends IService<MeteringPointDataYear> {
    void save(Long pointId, LocalDateTime time, BigDecimal value);
    List<MeteringPointDataYear> findByDateAndPointIds(LocalDate date, List<Long> pointIds);
    List<MeteringPointDataYear> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds);
    MeteringPointDataYear findByDateAndPointId(LocalDate date, Long pointId);

}
