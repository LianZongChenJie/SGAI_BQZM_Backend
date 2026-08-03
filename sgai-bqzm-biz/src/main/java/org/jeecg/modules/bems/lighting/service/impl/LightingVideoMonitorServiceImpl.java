package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingVideoMonitor;
import org.jeecg.modules.bems.lighting.mapper.LightingVideoMonitorMapper;
import org.jeecg.modules.bems.lighting.service.ILightingVideoMonitorService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class LightingVideoMonitorServiceImpl extends ServiceImpl<LightingVideoMonitorMapper, LightingVideoMonitor> implements ILightingVideoMonitorService {

    @Override
    public IPage<LightingVideoMonitor> listPage(LightingVideoMonitor params, int pageNo, int pageSize) {
        LambdaQueryWrapper<LightingVideoMonitor> queryWrapper = new LambdaQueryWrapper<LightingVideoMonitor>()
                .like(StringUtils.isNotEmpty(params.getVideoName()), LightingVideoMonitor::getVideoName, params.getVideoName())
                .eq(params.getAreaId() != null, LightingVideoMonitor::getAreaId, params.getAreaId())
                .eq(StringUtils.isNotEmpty(params.getStatus()), LightingVideoMonitor::getStatus, params.getStatus())
                .orderByAsc(LightingVideoMonitor::getSort);
        return super.page(new Page<>(pageNo, pageSize), queryWrapper);
    }

    @Override
    public List<LightingVideoMonitor> listByArea(Long areaId) {
        return super.list(new LambdaQueryWrapper<LightingVideoMonitor>()
                .eq(LightingVideoMonitor::getAreaId, areaId)
                .orderByAsc(LightingVideoMonitor::getSort));
    }

    /**
     * 覆盖 IService.list()：按 sort 升序返回全部视频（设备监控页卡片顺序稳定）
     */
    @Override
    public List<LightingVideoMonitor> list() {
        return super.list(new LambdaQueryWrapper<LightingVideoMonitor>()
                .orderByAsc(LightingVideoMonitor::getSort));
    }
}
