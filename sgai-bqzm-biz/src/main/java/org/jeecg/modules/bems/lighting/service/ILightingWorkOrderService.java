package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingWorkOrder;

public interface ILightingWorkOrderService extends IService<LightingWorkOrder> {

    /**
     * 分页查询工单
     */
    IPage<LightingWorkOrder> listPage(LightingWorkOrder params, int pageNo, int pageSize);

    /**
     * 创建工单
     */
    void createOrder(LightingWorkOrder workOrder);

    /**
     * 处理工单
     */
    void handleOrder(Long id, String handleResult, String assignee);
}
