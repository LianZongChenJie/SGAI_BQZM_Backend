package org.jeecg.modules.bems.energyAnalysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataDay;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MeteringPointDataDayMapper extends BaseMapper<MeteringPointDataDay> {
    BigDecimal findAvgByLtTimeAndPointId(@Param("date") LocalDateTime dateTime, @Param("pointId") Long pointId);
}
