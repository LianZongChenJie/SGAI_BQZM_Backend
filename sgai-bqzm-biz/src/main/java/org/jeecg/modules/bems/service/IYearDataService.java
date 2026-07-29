package org.jeecg.modules.bems.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.entity.YearData;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface IYearDataService extends IService<YearData> {
    List<YearData> findByTime(LocalDateTime year);
    YearData findByDeviceIdAndTime(Long deviceId, LocalDateTime time);

    List<YearData> findByDeviceIdsAndTime(Collection<Long> deviceIds, LocalDateTime time);

    boolean saveOrUpdate(YearData entity);

    List<YearData> findByDeviceIdsAndTimeRange(Collection<Long> deviceIds, LocalDateTime startTime, LocalDateTime endTime);
}
