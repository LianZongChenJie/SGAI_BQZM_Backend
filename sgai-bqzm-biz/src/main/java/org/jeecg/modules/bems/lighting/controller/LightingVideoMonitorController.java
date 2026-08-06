package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingVideoMonitor;
import org.jeecg.modules.bems.lighting.service.ILightingVideoMonitorService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 视频监控
 */
@Api(tags = "照明-视频监控")
@RestController
@RequestMapping("/bems/lighting/videoMonitor")
@AllArgsConstructor
public class LightingVideoMonitorController {

    private final ILightingVideoMonitorService service;

    /**
     * 分页查询视频监控
     */
    @ApiOperation("分页查询视频监控")
    @GetMapping("/listPage")
    public Result<?> listPage(LightingVideoMonitor params,
                              @RequestParam(defaultValue = "1") int pageNo,
                              @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(service.listPage(params, pageNo, pageSize));
    }

    /**
     * 获取所有视频列表
     */
    @ApiOperation("获取所有视频列表")
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(service.list());
    }

    /**
     * 根据区域查询视频列表
     */
    @ApiOperation("根据区域查询视频列表")
    @GetMapping("/listByArea")
    public Result<?> listByArea(Long areaId) {
        return Result.ok(service.listByArea(areaId));
    }

    /**
     * 根据空间查询视频列表
     * spaceId 与 lighting_area.space 对应（如 1/2/3/4/901/902，表内按数字存储），
     * 兼容前端直接传字符串 space 值。
     */
    @ApiOperation("根据空间查询视频列表（spaceId 对应 lighting_area.space）")
    @GetMapping("/listBySpace")
    public Result<?> listBySpace(String spaceId) {
        if (StringUtils.isBlank(spaceId)) {
            return Result.ok(Collections.emptyList());
        }
        try {
            return Result.ok(service.listBySpace(Long.valueOf(spaceId.trim())));
        } catch (NumberFormatException e) {
            return Result.ok(Collections.emptyList());
        }
    }

    /**
     * 获取视频详情
     */
    @ApiOperation("获取视频详情")
    @GetMapping("/detail")
    public Result<?> detail(Long id) {
        return Result.ok(service.getById(id));
    }

    /**
     * 新增视频监控
     */
    @ApiOperation("新增视频监控")
    @PostMapping("/add")
    public Result<?> add(@RequestBody LightingVideoMonitor videoMonitor) {
        service.save(videoMonitor);
        return Result.ok("新增成功");
    }

    /**
     * 更新视频监控
     */
    @ApiOperation("更新视频监控")
    @PostMapping("/update")
    public Result<?> update(@RequestBody LightingVideoMonitor videoMonitor) {
        service.updateById(videoMonitor);
        return Result.ok("更新成功");
    }

    /**
     * 删除视频监控
     */
    @ApiOperation("删除视频监控")
    @PostMapping("/delete")
    public Result<?> delete(Long id) {
        service.removeById(id);
        return Result.ok("删除成功");
    }
}
