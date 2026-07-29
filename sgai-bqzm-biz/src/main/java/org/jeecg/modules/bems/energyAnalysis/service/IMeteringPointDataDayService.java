package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataDay;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IMeteringPointDataDayService extends IService<MeteringPointDataDay> {

    List<MeteringPointDataDay> findByTimeRangeAndPointIds(LocalDateTime startTime, LocalDateTime endTime, List<Long> pointIds);
    void save(Long pointId, LocalDateTime time, BigDecimal value);
    List<MeteringPointDataDay> findByDateAndPointIds(LocalDate date, List<Long> pointIds);
    MeteringPointDataDay findByDateAndPointId(LocalDate date, Long pointId);
    List<MeteringPointDataDay> findByTimeRangeAndPointId(LocalDate startDate, LocalDate endDate, Long pointId);

    /**
     * 查询平均值，小于时间的数据
     * @param date 时间
     * @param pointId 计量规则点位id
     */
    BigDecimal findAvgByLtTimeAndPointId(LocalDate date,Long pointId);
}
