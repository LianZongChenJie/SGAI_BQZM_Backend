package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import cn.hutool.core.collection.CollectionUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.entity.LightingSceneDetail;
import org.jeecg.modules.bems.lighting.mapper.LightingSceneDetailMapper;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.jeecg.modules.bems.lighting.vo.LightingDistrictVo;
import org.jeecg.modules.bems.permission.annotation.DataPermission;
import org.springframework.beans.BeanUtils;
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

    private final LightingSceneDetailMapper sceneDetailMapper;

    /**
     * 分页查询片区列表
     */
    @ApiOperation("分页查询片区列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNo", value = "页码", defaultValue = "1", paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页条数", defaultValue = "10", paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "districtName", value = "片区名称", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "districtCode", value = "片区编码", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "status", value = "状态：启用、停用", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "type", value = "类型", paramType = "query", dataType = "string")
    })
    @DataPermission
    @GetMapping("/listPage")
    public Result<IPage<LightingDistrictVo>> listPage(@RequestParam(defaultValue = "1") Integer pageNo,
                                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                                      String districtName,
                                                      String districtCode,
                                                      String status,
                                                      String type) {
        LambdaQueryWrapper<LightingDistrict> queryWrapper = new LambdaQueryWrapper<LightingDistrict>()
                .like(StringUtils.isNotEmpty(districtName), LightingDistrict::getDistrictName, districtName)
                .like(StringUtils.isNotEmpty(districtCode), LightingDistrict::getDistrictCode, districtCode)
                .eq(StringUtils.isNotEmpty(status), LightingDistrict::getStatus, status)
                .eq(StringUtils.isNotEmpty(type), LightingDistrict::getType, type)
                .orderByAsc(LightingDistrict::getSort);
        Page<LightingDistrict> page = service.page(new Page<>(pageNo, pageSize), queryWrapper);

        // 收集当前页片区的 sceneId 集合（实体为 String），解析为 Long 后批量查询场景明细（避免逐条查询的 N+1）
        Set<Long> sceneIds = page.getRecords().stream()
                .map(LightingDistrict::getSceneId)
                .filter(StringUtils::isNotEmpty)
                .map(s -> {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        // 场景明细按场景id分组，key 统一转字符串，便于与片区 sceneId 直接匹配
        final Map<String, List<LightingSceneDetail>> detailMap;
        if (sceneIds.isEmpty()) {
            detailMap = Collections.emptyMap();
        } else {
            detailMap = sceneDetailMapper.selectList(
                            new LambdaQueryWrapper<LightingSceneDetail>().in(LightingSceneDetail::getSceneId, sceneIds))
                    .stream()
                    .collect(Collectors.groupingBy(d -> String.valueOf(d.getSceneId())));
        }

        // 实体 → VO 数据转换；relType/relIds 从场景明细聚合（与场景详情/列表返回一致）
        List<LightingDistrictVo> records = page.getRecords().stream().map(district -> {
            LightingDistrictVo vo = new LightingDistrictVo();
            BeanUtils.copyProperties(district, vo);
            fillSceneRel(detailMap.get(district.getSceneId()), vo);
            return vo;
        }).collect(Collectors.toList());

        Page<LightingDistrictVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return Result.ok(result);
    }

    /**
     * 根据场景明细聚合 relType/relIds 并填充到 VO（与场景详情/场景列表返回的数据一致）：
     * - relType 取场景下第一条明细的 relType（场景设计上明细类型统一）
     * - relIds 为场景下所有明细 relId 逗号拼接
     */
    private void fillSceneRel(List<LightingSceneDetail> details, LightingDistrictVo vo) {
        if (CollectionUtil.isEmpty(details)) {
            return;
        }
        LightingSceneDetail first = details.get(0);
        vo.setRelType(first.getRelType());
        vo.setRelIds(details.stream()
                .map(d -> String.valueOf(d.getRelId()))
                .collect(Collectors.joining(",")));
    }

    /**
     * 获取所有片区
     */
    @ApiOperation("获取所有片区")
    @DataPermission
    @GetMapping("/all")
    public Result<?> all() {
        return Result.ok(service.list(new LambdaQueryWrapper<LightingDistrict>().orderByAsc(LightingDistrict::getSort)));
    }

    /**
     * 获取片区详情
     */
    @ApiOperation("获取片区详情")
    @DataPermission
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
    @DataPermission
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
