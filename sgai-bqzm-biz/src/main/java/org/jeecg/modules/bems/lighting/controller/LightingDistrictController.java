package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 照明-片区管理（片区与区域一对多，区域通过 districtId 关联片区）
 */
@Slf4j
@Api(tags = "照明-片区")
@RestController
@RequestMapping("/bems/lighting/district")
@AllArgsConstructor
public class LightingDistrictController {

    private final ILightingDistrictService service;

    private final ILightingAreaService areaService;

    private final ILightingCircuitService circuitService;

    /**
     * 分页查询片区列表
     */
    @ApiOperation("分页查询片区列表")
    @GetMapping("/listPage")
    public Result<IPage<LightingDistrict>> listPage(@RequestParam(defaultValue = "1") Integer pageNo,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    String districtName,
                                                    String districtCode,
                                                    String status) {
        LambdaQueryWrapper<LightingDistrict> queryWrapper = new LambdaQueryWrapper<LightingDistrict>()
                .like(StringUtils.isNotEmpty(districtName), LightingDistrict::getDistrictName, districtName)
                .like(StringUtils.isNotEmpty(districtCode), LightingDistrict::getDistrictCode, districtCode)
                .eq(StringUtils.isNotEmpty(status), LightingDistrict::getStatus, status)
                .orderByAsc(LightingDistrict::getSort);
        return Result.ok(service.page(new Page<>(pageNo, pageSize), queryWrapper));
    }

    /**
     * 获取所有片区
     */
    @ApiOperation("获取所有片区")
    @GetMapping("/all")
    public Result<?> all() {
        return Result.ok(service.list(new LambdaQueryWrapper<LightingDistrict>().orderByAsc(LightingDistrict::getSort)));
    }

    /**
     * 获取片区详情
     */
    @ApiOperation("获取片区详情")
    @GetMapping("/detail")
    public Result<?> detail(Long id) {
        return Result.ok(service.getById(id));
    }

    /**
     * 查询片区下所有回路信息
     * <p>
     * 通过片区 spaceIds（逗号分隔的空间编码，如 1,2,3）→ lighting_area.space → 区域ID集合
     * → lighting_circuit.area_id 关联查询该片区下所有回路
     */
    @ApiOperation("查询片区下所有回路信息（片区spaceIds → lighting_area.space → lighting_circuit.area_id）")
    @GetMapping("/circuits")
    public Result<List<LightingCircuit>> circuits(Long id) {
        LightingDistrict district = service.getById(id);
        if (district == null) {
            return Result.error("片区不存在");
        }
        if (StringUtils.isEmpty(district.getSpaceIds())) {
            return Result.ok(Collections.emptyList());
        }
        // 解析 spaceIds（逗号分隔的空间编码，去空格、去空、去重）
        List<String> spaces = Arrays.stream(district.getSpaceIds().split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        if (spaces.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 空间编码 → 区域集合
        List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .in(LightingArea::getSpace, spaces));
        if (areas.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        Set<Long> areaIds = areas.stream().map(LightingArea::getId).collect(Collectors.toSet());
        Map<Long, LightingArea> areaMap = areas.stream()
                .collect(Collectors.toMap(LightingArea::getId, Function.identity()));
        // 区域ID集合 → 回路集合
        List<LightingCircuit> circuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                .in(LightingCircuit::getAreaId, areaIds)
                .orderByAsc(LightingCircuit::getId));
        // 回填区域名称、空间名称
        for (LightingCircuit circuit : circuits) {
            LightingArea area = areaMap.get(circuit.getAreaId());
            if (area != null) {
                circuit.setAreaName(area.getAreaName());
                circuit.setSpaceName(area.getSpaceName());
            }
        }
        return Result.ok(circuits);
    }

    /**
     * 新增片区
     */
    @ApiOperation("新增片区")
    @PostMapping("/add")
    public Result<?> add(@RequestBody LightingDistrict district) {
        service.save(district);
        return Result.ok("新增成功");
    }

    /**
     * 编辑片区
     */
    @ApiOperation("编辑片区")
    @PostMapping("/update")
    public Result<?> update(@RequestBody LightingDistrict district) {
        service.updateById(district);
        return Result.ok("更新成功");
    }

    /**
     * 删除片区
     */
    @ApiOperation("删除片区")
    @PostMapping("/delete")
    public Result<?> delete(Long id) {
        service.removeById(id);
        return Result.ok("删除成功");
    }
}
