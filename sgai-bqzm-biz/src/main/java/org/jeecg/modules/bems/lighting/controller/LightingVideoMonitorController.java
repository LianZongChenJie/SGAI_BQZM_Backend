package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.entity.LightingVideoMonitor;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.jeecg.modules.bems.lighting.service.ILightingVideoMonitorService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 视频监控
 */
@Api(tags = "照明-视频监控")
@RestController
@RequestMapping("/bems/lighting/videoMonitor")
@AllArgsConstructor
public class LightingVideoMonitorController {

    private final ILightingVideoMonitorService service;

    private final ILightingDistrictService districtService;

    private final ILightingAreaService areaService;

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
     * 获取所有视频列表（数据来源：lighting_district(type=1) 与 lighting_area 合并，不查 lighting_video_monitor）
     * 名称取片区名称/区域名称，视频地址取 monitorAdr，仅返回有监控地址的记录。
     */
    @ApiOperation("获取所有视频列表（片区type=1 + 区域合并）")
    @GetMapping("/listAll")
    public Result<List<LightingVideoMonitor>> listAll() {
        List<LightingVideoMonitor> result = new ArrayList<>();

        // 片区：type=1 且有监控地址；areaName/videoName 均取片区名称，areaId 取片区id
        districtService.list(new LambdaQueryWrapper<LightingDistrict>()
                        .eq(LightingDistrict::getType, "1")
                        .isNotNull(LightingDistrict::getMonitorAdr))
                .forEach(d -> {
                    if (StringUtils.isNotBlank(d.getMonitorAdr())) {
                        result.add(buildMonitor(d.getId(), d.getDistrictName(), d.getDistrictName(), d.getMonitorAdr()));
                    }
                });

        // 区域：有监控地址；videoName 取区域名称，areaName 取区域所属片区的名称（通过 district_id 反查），areaId 取区域id
        List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .isNotNull(LightingArea::getMonitorAdr));
        // 批量反查片区名称，避免 N+1
        Set<Long> districtIds = areas.stream()
                .map(LightingArea::getDistrictId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> districtNameMap = districtIds.isEmpty()
                ? Collections.emptyMap()
                : districtService.listByIds(districtIds).stream()
                        .collect(Collectors.toMap(LightingDistrict::getId, LightingDistrict::getDistrictName, (x, y) -> x));

        for (LightingArea a : areas) {
            if (StringUtils.isNotBlank(a.getMonitorAdr())) {
                String districtName = a.getDistrictId() == null ? null : districtNameMap.get(a.getDistrictId());
                result.add(buildMonitor(a.getId(), a.getAreaName(), districtName, a.getMonitorAdr()));
            }
        }

        return Result.ok(result);
    }

    /**
     * 用原实体组装视频监控数据：
     * areaId=来源主键，videoName=视频名称，areaName=区域（片区）名称，videoAddress=视频地址
     */
    private LightingVideoMonitor buildMonitor(Long id, String videoName, String areaName, String videoAddress) {
        LightingVideoMonitor monitor = new LightingVideoMonitor();
        monitor.setId(id);
        monitor.setAreaId(id);
        monitor.setAreaName(areaName);
        monitor.setVideoName(videoName);
        monitor.setVideoAddress(videoAddress);
        monitor.setStatus("在线");
        return monitor;
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
