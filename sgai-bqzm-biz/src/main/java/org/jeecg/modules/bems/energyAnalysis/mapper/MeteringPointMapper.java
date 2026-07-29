package org.jeecg.modules.bems.energyAnalysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.bems.energyAnalysis.dto.MeteringPointDto;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointVo;

import java.util.Set;

public interface MeteringPointMapper extends BaseMapper<MeteringPoint> {

    IPage<MeteringPointVo> selectMeteringPoint(
        IPage<MeteringPointVo> page,
        MeteringPointDto params,
        @Param("categoryIds") Set<Long> categoryIds,
        @Param("spaceIds") Set<Long> spaceIds
    );
}
