package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingFaultRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ILightingFaultRecordService extends IService<LightingFaultRecord> {

    /**
     * 分页查询故障记录
     */
    IPage<LightingFaultRecord> listPage(LightingFaultRecord params, int pageNo, int pageSize);

    /**
     * 故障类型统计
     */
    List<Map<String, Object>> countByFaultType(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 故障趋势统计
     */
    List<Map<String, Object>> countByDate(LocalDateTime startTime, LocalDateTime endTime);
}
