package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingDataInterface;

import java.util.List;

public interface ILightingDataInterfaceService extends IService<LightingDataInterface> {

    /**
     * 分页查询数据采集接口
     */
    IPage<LightingDataInterface> listPage(LightingDataInterface params, int pageNo, int pageSize);

    /**
     * 根据状态查询接口列表
     */
    List<LightingDataInterface> listByStatus(String status);

    /**
     * 更新接口状态
     */
    void updateStatus(Long id, String status);
}
