package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingReportConfig;
import org.jeecg.modules.bems.lighting.mapper.LightingReportConfigMapper;
import org.jeecg.modules.bems.lighting.service.ILightingReportConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class LightingReportConfigServiceImpl extends ServiceImpl<LightingReportConfigMapper, LightingReportConfig> implements ILightingReportConfigService {

    @Override
    public IPage<LightingReportConfig> listPage(LightingReportConfig params, int pageNo, int pageSize) {
        LambdaQueryWrapper<LightingReportConfig> queryWrapper = new LambdaQueryWrapper<LightingReportConfig>()
                .like(StringUtils.isNotEmpty(params.getReportName()), LightingReportConfig::getReportName, params.getReportName())
                .eq(StringUtils.isNotEmpty(params.getReportType()), LightingReportConfig::getReportType, params.getReportType())
                .eq(StringUtils.isNotEmpty(params.getStatus()), LightingReportConfig::getStatus, params.getStatus())
                .orderByAsc(LightingReportConfig::getSort);
        return super.page(new Page<>(pageNo, pageSize), queryWrapper);
    }

    @Override
    public List<LightingReportConfig> listByType(String reportType) {
        return super.list(new LambdaQueryWrapper<LightingReportConfig>()
                .eq(LightingReportConfig::getReportType, reportType)
                .eq(LightingReportConfig::getStatus, "启用")
                .orderByAsc(LightingReportConfig::getSort));
    }
}
