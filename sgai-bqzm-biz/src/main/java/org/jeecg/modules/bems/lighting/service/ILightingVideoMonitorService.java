package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingVideoMonitor;

import java.util.List;

public interface ILightingVideoMonitorService extends IService<LightingVideoMonitor> {

    /**
     * 分页查询视频监控
     */
    IPage<LightingVideoMonitor> listPage(LightingVideoMonitor params, int pageNo, int pageSize);

    /**
     * 根据区域查询视频列表
     */
    List<LightingVideoMonitor> listByArea(Long areaId);

    /**
     * 根据空间查询视频列表（按 sort 升序）
     */
    List<LightingVideoMonitor> listBySpace(Long spaceId);
}
