package org.jeecg.modules.bems.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.dto.DeviceDataFindDto;
import org.jeecg.modules.bems.entity.HourData;
import org.jeecg.modules.bems.vo.HourDataVo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface IHourDataService extends IService<HourData> {

    HourData findByDeviceIdAndTime(Long deviceId, LocalDateTime time);

    IPage<HourDataVo> listPage(DeviceDataFindDto params);

    List<HourData> findByDeviceIdAndTimes(Long deviceId,List<LocalDateTime> times);

    List<HourData> findByTime(LocalDateTime hour);

    List<HourData> findByDeviceIdAndTimeRange(Long deiceId,LocalDateTime startTime,LocalDateTime endTime);

    void setStartValueAndEndValue(LocalDate month);

    List<HourData> findByDeviceIdsAndTime(Collection<Long> deviceIds, LocalDateTime time);

    List<HourData> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    boolean saveOrUpdate(HourData entity);

    void preGeneration(List<Long> deviceIds, LocalDate date);

    boolean updateById(HourData entity);

    List<HourData> findByDeviceIdsAndTimeRange(Collection<Long> deviceIds, LocalDateTime startTime, LocalDateTime endTime);
}
