package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.lighting.entity.LightingWorkOrder;
import org.jeecg.modules.bems.lighting.mapper.LightingWorkOrderMapper;
import org.jeecg.modules.bems.lighting.service.ILightingWorkOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@AllArgsConstructor
public class LightingWorkOrderServiceImpl extends ServiceImpl<LightingWorkOrderMapper, LightingWorkOrder> implements ILightingWorkOrderService {

    @Override
    public IPage<LightingWorkOrder> listPage(LightingWorkOrder params, int pageNo, int pageSize) {
        LambdaQueryWrapper<LightingWorkOrder> queryWrapper = new LambdaQueryWrapper<LightingWorkOrder>()
                .like(StringUtils.isNotEmpty(params.getTitle()), LightingWorkOrder::getTitle, params.getTitle())
                .eq(StringUtils.isNotEmpty(params.getStatus()), LightingWorkOrder::getStatus, params.getStatus())
                .eq(StringUtils.isNotEmpty(params.getPriority()), LightingWorkOrder::getPriority, params.getPriority())
                .eq(StringUtils.isNotEmpty(params.getSource()), LightingWorkOrder::getSource, params.getSource())
                .orderByDesc(LightingWorkOrder::getCreateTime);
        return super.page(new Page<>(pageNo, pageSize), queryWrapper);
    }

    @Override
    @Transactional
    public void createOrder(LightingWorkOrder workOrder) {
        // 生成工单编号
        String workOrderNo = "WO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" +
                String.format("%03d", count() + 1);
        workOrder.setWorkOrderNo(workOrderNo);
        workOrder.setStatus("待处理");
        workOrder.setCreateTime(LocalDateTime.now());
        super.save(workOrder);
    }

    @Override
    @Transactional
    public void handleOrder(Long id, String handleResult, String assignee) {
        LightingWorkOrder order = super.getById(id);
        if (order == null) {
            throw new JeecgBootException("工单不存在");
        }
        order.setStatus("已完成");
        order.setHandleResult(handleResult);
        order.setHandleTime(LocalDateTime.now());
        if (StringUtils.isNotEmpty(assignee)) {
            order.setAssigneeName(assignee);
        }
        super.updateById(order);
    }
}
