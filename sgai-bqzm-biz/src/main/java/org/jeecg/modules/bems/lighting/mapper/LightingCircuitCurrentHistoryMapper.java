package org.jeecg.modules.bems.lighting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitCurrentHistory;

/**
 * 回路电流整点采样历史 Mapper
 */
@Mapper
public interface LightingCircuitCurrentHistoryMapper extends BaseMapper<LightingCircuitCurrentHistory> {
}
