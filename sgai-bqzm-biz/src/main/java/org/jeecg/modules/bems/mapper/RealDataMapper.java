package org.jeecg.modules.bems.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jeecg.modules.bems.entity.RealData;

import java.time.LocalDateTime;
import java.util.List;

public interface RealDataMapper extends BaseMapper<RealData> {

    List<RealData> findFirstByTimeRangeDesc(LocalDateTime startTime,LocalDateTime endTime);

    List<RealData> findFirstByTimeRangeAsc(LocalDateTime startTime,LocalDateTime endTime);

    List<RealData> findFirstByLtTimeDesc(LocalDateTime startTime, LocalDateTime endTime);
}
