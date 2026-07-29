package org.jeecg.modules.bems.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.entity.DayData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface IDayDataService extends IService<DayData> {

    List<DayData> findByTime(LocalDateTime day);

    DayData findByDeviceIdAndTime(Long deviceId, LocalDateTime time);

    List<DayData> findByDeviceIdsAndTime(Collection<Long> deviceIds, LocalDateTime time);

    void preGeneration(List<Long>deviceIds, LocalDate date);

    boolean saveOrUpdate(DayData dayData);

    List<DayData> findByDeviceIdsAndTimeRange(Collection<Long> deviceIds, LocalDateTime startTime, LocalDateTime endTime);
}
