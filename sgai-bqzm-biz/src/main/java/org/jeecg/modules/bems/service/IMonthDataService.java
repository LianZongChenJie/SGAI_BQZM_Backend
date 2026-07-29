package org.jeecg.modules.bems.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.entity.MonthData;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface IMonthDataService extends IService<MonthData> {
    List<MonthData> findByTime(LocalDateTime month);
    MonthData findByDeviceIdAndTime(Long deviceId, LocalDateTime time);

    List<MonthData> findByDeviceIdsAndTime(Collection<Long> deviceIds, LocalDateTime time);

    boolean saveOrUpdate(MonthData entity);

    List<MonthData> findByDeviceIdsAndTimeRange(Collection<Long> deviceIds, LocalDateTime startTime, LocalDateTime endTime);

}
