package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingWorkOrder;
import org.jeecg.modules.bems.lighting.service.ILightingWorkOrderService;
import org.springframework.web.bind.annotation.*;

/**
 * 工单管理
 */
@Api(tags = "照明-工单管理")
@RestController
@RequestMapping("/bems/lighting/workOrder")
@AllArgsConstructor
public class LightingWorkOrderController {

    private final ILightingWorkOrderService service;

    /**
     * 分页查询工单
     */
    @ApiOperation("分页查询工单")
    @GetMapping("/listPage")
    public Result<?> listPage(LightingWorkOrder params,
                              @RequestParam(defaultValue = "1") int pageNo,
                              @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(service.listPage(params, pageNo, pageSize));
    }

    /**
     * 获取工单详情
     */
    @ApiOperation("获取工单详情")
    @GetMapping("/detail")
    public Result<?> detail(Long id) {
        return Result.ok(service.getById(id));
    }

    /**
     * 创建工单
     */
    @ApiOperation("创建工单")
    @PostMapping("/create")
    public Result<?> create(@RequestBody LightingWorkOrder workOrder) {
        service.createOrder(workOrder);
        return Result.ok("创建成功");
    }

    /**
     * 处理工单
     */
    @ApiOperation("处理工单")
    @PostMapping("/handle")
    public Result<?> handle(Long id, String handleResult, String assignee) {
        service.handleOrder(id, handleResult, assignee);
        return Result.ok("处理成功");
    }

    /**
     * 更新工单
     */
    @ApiOperation("更新工单")
    @PostMapping("/update")
    public Result<?> update(@RequestBody LightingWorkOrder workOrder) {
        service.updateById(workOrder);
        return Result.ok("更新成功");
    }

    /**
     * 删除工单
     */
    @ApiOperation("删除工单")
    @PostMapping("/delete")
    public Result<?> delete(Long id) {
        service.removeById(id);
        return Result.ok("删除成功");
    }
}
