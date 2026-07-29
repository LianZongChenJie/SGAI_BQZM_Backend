package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataMinute;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IMeteringPointDataMinuteService extends IService<MeteringPointDataMinute> {
    void save(Long pointId, LocalDateTime time, BigDecimal value);

    List<MeteringPointDataMinute> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds);

}
