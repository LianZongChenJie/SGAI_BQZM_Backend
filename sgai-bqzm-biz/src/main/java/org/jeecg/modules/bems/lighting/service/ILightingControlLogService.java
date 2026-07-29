package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingControlLog;

public interface ILightingControlLogService extends IService<LightingControlLog> {

    /**
     * 分页查询控制日志
     */
    IPage<LightingControlLog> listPage(LightingControlLog params, int pageNo, int pageSize);

    /**
     * 记录控制日志
     */
    void saveLog(String controlType, Long relId, String relName, String operation, String operatorType, String operatorBy, String result);
}
