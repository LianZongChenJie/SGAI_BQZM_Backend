package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingDiagRule;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDiagRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 照明智能诊断-规则与报警
 */
@Api(tags = "照明-智能诊断(规则/报警)")
@Slf4j
@RestController
@RequestMapping("/bems/lighting/diagRule")
@AllArgsConstructor
public class LightingDiagRuleController {

    /** 北区公区空间编码 */
    private static final String SPACE_BQ = "903";

    private final ILightingDiagRuleService diagRuleService;

    private final ILightingCircuitService circuitService;

    private final ILightingAreaService areaService;

    @ApiOperation("规则清单（按启用，可按类型分类过滤：数据/供电/线路/负载/控制）")
    @GetMapping("/list")
    public Result<List<LightingDiagRule>> list(@RequestParam(required = false) String domain) {
        return Result.ok(diagRuleService.listEnabled(domain));
    }

    @ApiOperation("规则类型分类清单（用于前端筛选）")
    @GetMapping("/domains")
    public Result<List<String>> domains() {
        List<String> domains = diagRuleService.listEnabled(null).stream()
                .map(LightingDiagRule::getRuleDomain)
                .distinct()
                .collect(Collectors.toList());
        return Result.ok(domains);
    }

    @ApiOperation("查询 903 空间报警回路列表（含区域/电流/额定电流/命中规则）")
    @GetMapping("/alarmCircuits")
    public Result<List<LightingCircuit>> alarmCircuits() {
        // 先取 903 空间下的区域集合
        List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getSpace, SPACE_BQ));
        if (areas == null || areas.isEmpty()) {
            return Result.ok(java.util.Collections.emptyList());
        }
        Set<Long> areaIds = areas.stream().map(LightingArea::getId).collect(Collectors.toSet());
        // 查询这些区域下被标记为报警的回路
        List<LightingCircuit> circuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                .in(LightingCircuit::getAreaId, areaIds)
                .eq(LightingCircuit::getAlarmFlag, "报警"));
        if (circuits.isEmpty()) {
            return Result.ok(circuits);
        }
        // 回填区域名称/空间名称
        Map<Long, LightingArea> areaMap = areas.stream()
                .collect(Collectors.toMap(LightingArea::getId, Function.identity()));
        for (LightingCircuit circuit : circuits) {
            LightingArea area = areaMap.get(circuit.getAreaId());
            if (area != null) {
                circuit.setAreaName(area.getAreaName());
                circuit.setSpaceName(area.getSpaceName());
            }
        }
        return Result.ok(circuits);
    }

    @ApiOperation("903 空间报警数量统计")
    @GetMapping("/alarmCount")
    public Result<Long> alarmCount() {
        List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getSpace, SPACE_BQ));
        if (areas == null || areas.isEmpty()) {
            return Result.ok(0L);
        }
        Set<Long> areaIds = areas.stream().map(LightingArea::getId).collect(Collectors.toSet());
        Long count = circuitService.count(new LambdaQueryWrapper<LightingCircuit>()
                .in(LightingCircuit::getAreaId, areaIds)
                .eq(LightingCircuit::getAlarmFlag, "报警"));
        return Result.ok(count);
    }
}
