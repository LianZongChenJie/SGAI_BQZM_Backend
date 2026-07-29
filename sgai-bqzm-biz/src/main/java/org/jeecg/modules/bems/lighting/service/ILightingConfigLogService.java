package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingConfigLog;

public interface ILightingConfigLogService extends IService<LightingConfigLog> {

    /**
     * 分页查询配置日志
     */
    IPage<LightingConfigLog> listPage(LightingConfigLog params, int pageNo, int pageSize);

    /**
     * 记录配置日志
     */
    void saveLog(String operType, String operModule, String targetType, Long targetId, String targetName, String operContent);
}
