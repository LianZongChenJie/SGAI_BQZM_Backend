package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingReportConfig;
import org.jeecg.modules.bems.lighting.service.ILightingReportConfigService;
import org.springframework.web.bind.annotation.*;

/**
 * 报表配置
 */
@Api(tags = "照明-报表配置")
@RestController
@RequestMapping("/bems/lighting/reportConfig")
@AllArgsConstructor
public class LightingReportConfigController {

    private final ILightingReportConfigService service;

    /**
     * 分页查询报表配置
     */
    @ApiOperation("分页查询报表配置")
    @GetMapping("/listPage")
    public Result<?> listPage(LightingReportConfig params,
                              @RequestParam(defaultValue = "1") int pageNo,
                              @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(service.listPage(params, pageNo, pageSize));
    }

    /**
     * 获取所有报表列表
     */
    @ApiOperation("获取所有报表列表")
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(service.list());
    }

    /**
     * 根据类型查询报表列表
     */
    @ApiOperation("根据类型查询报表列表")
    @GetMapping("/listByType")
    public Result<?> listByType(String reportType) {
        return Result.ok(service.listByType(reportType));
    }

    /**
     * 获取报表详情
     */
    @ApiOperation("获取报表详情")
    @GetMapping("/detail")
    public Result<?> detail(Long id) {
        return Result.ok(service.getById(id));
    }

    /**
     * 新增报表配置
     */
    @ApiOperation("新增报表配置")
    @PostMapping("/add")
    public Result<?> add(@RequestBody LightingReportConfig reportConfig) {
        service.save(reportConfig);
        return Result.ok("新增成功");
    }

    /**
     * 更新报表配置
     */
    @ApiOperation("更新报表配置")
    @PostMapping("/update")
    public Result<?> update(@RequestBody LightingReportConfig reportConfig) {
        service.updateById(reportConfig);
        return Result.ok("更新成功");
    }

    /**
     * 删除报表配置
     */
    @ApiOperation("删除报表配置")
    @PostMapping("/delete")
    public Result<?> delete(Long id) {
        service.removeById(id);
        return Result.ok("删除成功");
    }
}
