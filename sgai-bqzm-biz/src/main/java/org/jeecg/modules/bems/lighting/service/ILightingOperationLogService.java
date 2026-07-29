package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.dto.LightingOperationLogQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;

import java.time.LocalDateTime;

public interface ILightingOperationLogService extends IService<LightingOperationLog> {

    /**
     * 保存操作记录
     * @param relType 关联类型
     * @param relId 关联id
     * @param name 名称
     * @param time 时间
     * @param operationType 操作类型
     */
    void saveLog(String relType, Long relId, String name, LocalDateTime time, String operationType);

    IPage<LightingOperationLog> listPage(LightingOperationLogQueryDto params);
}
