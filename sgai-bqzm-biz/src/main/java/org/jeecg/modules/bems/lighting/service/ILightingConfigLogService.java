package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.dto.LightingConfigLogQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingConfigLog;

public interface ILightingConfigLogService extends IService<LightingConfigLog> {

    /**
     * 分页查询配置日志。
     * 支持：操作类型/操作模块/操作人账号/操作对象类型/操作对象ID（精确），
     * 操作人姓名/操作对象名称/操作内容（模糊），操作时间段（含边界）。
     */
    IPage<LightingConfigLog> listPage(LightingConfigLogQueryDto params);

    /**
     * 记录配置日志
     */
    void saveLog(String operType, String operModule, String targetType, Long targetId, String targetName, String operContent);
}
