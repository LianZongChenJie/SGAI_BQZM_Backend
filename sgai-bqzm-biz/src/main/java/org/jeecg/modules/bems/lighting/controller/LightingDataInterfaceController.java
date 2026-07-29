package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingDataInterface;
import org.jeecg.modules.bems.lighting.service.ILightingDataInterfaceService;
import org.springframework.web.bind.annotation.*;

/**
 * 数据采集接口
 */
@Api(tags = "照明-数据采集接口")
@RestController
@RequestMapping("/bems/lighting/dataInterface")
@AllArgsConstructor
public class LightingDataInterfaceController {

    private final ILightingDataInterfaceService service;

    /**
     * 分页查询数据采集接口
     */
    @ApiOperation("分页查询数据采集接口")
    @GetMapping("/listPage")
    public Result<?> listPage(LightingDataInterface params,
                              @RequestParam(defaultValue = "1") int pageNo,
                              @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(service.listPage(params, pageNo, pageSize));
    }

    /**
     * 获取所有接口列表
     */
    @ApiOperation("获取所有接口列表")
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(service.list());
    }

    /**
     * 根据状态查询接口列表
     */
    @ApiOperation("根据状态查询接口列表")
    @GetMapping("/listByStatus")
    public Result<?> listByStatus(String status) {
        return Result.ok(service.listByStatus(status));
    }

    /**
     * 获取接口详情
     */
    @ApiOperation("获取接口详情")
    @GetMapping("/detail")
    public Result<?> detail(Long id) {
        return Result.ok(service.getById(id));
    }

    /**
     * 新增数据采集接口
     */
    @ApiOperation("新增数据采集接口")
    @PostMapping("/add")
    public Result<?> add(@RequestBody LightingDataInterface dataInterface) {
        service.save(dataInterface);
        return Result.ok("新增成功");
    }

    /**
     * 更新数据采集接口
     */
    @ApiOperation("更新数据采集接口")
    @PostMapping("/update")
    public Result<?> update(@RequestBody LightingDataInterface dataInterface) {
        service.updateById(dataInterface);
        return Result.ok("更新成功");
    }

    /**
     * 删除数据采集接口
     */
    @ApiOperation("删除数据采集接口")
    @PostMapping("/delete")
    public Result<?> delete(Long id) {
        service.removeById(id);
        return Result.ok("删除成功");
    }

    /**
     * 更新接口状态
     */
    @ApiOperation("更新接口状态")
    @PostMapping("/updateStatus")
    public Result<?> updateStatus(Long id, String status) {
        service.updateStatus(id, status);
        return Result.ok("更新成功");
    }
}
