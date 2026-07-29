package org.jeecg.modules.bems.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.entity.RealData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IRealDataService extends IService<RealData> {

    void save(Long deviceId, LocalDateTime time, BigDecimal value);

    List<RealData> findByTime(LocalDateTime time);

    RealData findByDeviceIdAndTime(Long deviceId, LocalDateTime time);

    List<RealData> findFirstByTimeRangeDesc(LocalDateTime startTime,LocalDateTime endTime);

    List<RealData> findFirstByTimeRangeAsc(LocalDateTime startTime,LocalDateTime endTime);

    List<RealData> findFirstByLtTimeDesc(LocalDateTime startTime, LocalDateTime endTime);

    List<RealData> findByDeviceIdAndTimeRange(Long deviceId, LocalDateTime startTime, LocalDateTime endTime);

    List<RealData> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    void preGeneration(List<Long> deviceIds,LocalDate date);
}
