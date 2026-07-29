package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingReportConfig;

import java.util.List;

public interface ILightingReportConfigService extends IService<LightingReportConfig> {

    /**
     * 分页查询报表配置
     */
    IPage<LightingReportConfig> listPage(LightingReportConfig params, int pageNo, int pageSize);

    /**
     * 根据类型查询报表列表
     */
    List<LightingReportConfig> listByType(String reportType);
}
