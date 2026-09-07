package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitAlarm;
import org.jeecg.modules.bems.lighting.entity.LightingDiagRule;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitAlarmService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDiagRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 照明智能诊断-规则与报警（报警数据以当前命中表 lighting_circuit_alarm 为准）
 */
@Api(tags = "照明-智能诊断(规则/报警)")
@Slf4j
@RestController
@RequestMapping("/bems/lighting/diagRule")
@AllArgsConstructor
public class LightingDiagRuleController {

    private final ILightingDiagRuleService diagRuleService;

    private final ILightingCircuitService circuitService;

    private final ILightingAreaService areaService;

    private final ILightingCircuitAlarmService alarmService;

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

    @ApiOperation("当前命中明细（全空间，一行=回路×规则，含级别；可按级别过滤 alarm/warn）")
    @GetMapping("/hits")
    public Result<List<LightingCircuitAlarm>> hits(
            @ApiParam("级别过滤：alarm报警/warn预警/note提示") @RequestParam(required = false) String level) {
        List<LightingCircuitAlarm> all = alarmService.list(new LambdaQueryWrapper<LightingCircuitAlarm>()
                .eq(level != null && !level.isEmpty(), LightingCircuitAlarm::getRuleLevel, level)
                .orderByDesc(LightingCircuitAlarm::getAlarmTime));
        return Result.ok(all == null ? Collections.emptyList() : all);
    }

    @ApiOperation("当前报警/预警回路列表（有任意命中的回路，全空间；含其命中规则）")
    @GetMapping("/alarmCircuits")
    public Result<List<Map<String, Object>>> alarmCircuits() {
        List<LightingCircuitAlarm> hits = alarmService.list(new LambdaQueryWrapper<LightingCircuitAlarm>());
        if (hits == null || hits.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 按 circuit 分组，得到每回路的命中规则列表
        Map<Long, List<LightingCircuitAlarm>> byCircuit = hits.stream()
                .collect(Collectors.groupingBy(LightingCircuitAlarm::getCircuitId));
        Set<Long> circuitIds = byCircuit.keySet();
        List<LightingCircuit> circuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                .in(LightingCircuit::getId, circuitIds));
        if (circuits == null) {
            circuits = Collections.emptyList();
        }
        // 区域映射
        Set<Long> areaIds = circuits.stream().map(LightingCircuit::getAreaId).collect(Collectors.toSet());
        List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>().in(LightingArea::getId, areaIds));
        Map<Long, LightingArea> areaMap = (areas == null ? Collections.<LightingArea>emptyList() : areas)
                .stream().collect(Collectors.toMap(LightingArea::getId, Function.identity()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (LightingCircuit c : circuits) {
            LightingArea area = areaMap.get(c.getAreaId());
            boolean hasAlarm = byCircuit.getOrDefault(c.getId(), Collections.emptyList()).stream()
                    .anyMatch(h -> "alarm".equals(h.getRuleLevel()));
            boolean hasWarn = byCircuit.getOrDefault(c.getId(), Collections.emptyList()).stream()
                    .anyMatch(h -> "warn".equals(h.getRuleLevel()));
            Map<String, Object> item = new HashMap<>();
            item.put("circuit", c);
            item.put("areaName", area != null ? area.getAreaName() : null);
            item.put("spaceName", area != null ? area.getSpaceName() : null);
            item.put("rules", byCircuit.get(c.getId()));
            item.put("hasAlarm", hasAlarm);
            item.put("hasWarn", hasWarn);
            result.add(item);
        }
        // 有报警/预警的排前
        result.sort((a, b) -> {
            boolean aa = (Boolean) a.get("hasAlarm");
            boolean ba = (Boolean) b.get("hasAlarm");
            return Boolean.compare(ba, aa);
        });
        return Result.ok(result);
    }

    @ApiOperation("当前命中回路数统计：报警级/预警级 回路数与命中规则数")
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        List<LightingCircuitAlarm> hits = alarmService.list(new LambdaQueryWrapper<LightingCircuitAlarm>());
        if (hits == null) {
            hits = Collections.emptyList();
        }
        Map<Long, Boolean> circuitHasAlarm = new HashMap<>();
        Map<Long, Boolean> circuitHasWarn = new HashMap<>();
        Map<String, Long> ruleCount = new HashMap<>();
        for (LightingCircuitAlarm h : hits) {
            if ("alarm".equals(h.getRuleLevel())) {
                circuitHasAlarm.put(h.getCircuitId(), true);
            }
            if ("warn".equals(h.getRuleLevel())) {
                circuitHasWarn.put(h.getCircuitId(), true);
            }
            ruleCount.merge(h.getRuleCode(), 1L, Long::sum);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("alarmCircuitCount", (long) circuitHasAlarm.size());
        map.put("warnCircuitCount", (long) circuitHasWarn.size());
        map.put("totalCircuitCount", (long) (circuitHasAlarm.keySet().size() + circuitHasWarn.size()));
        map.put("ruleCount", ruleCount);
        return Result.ok(map);
    }

    @ApiOperation("各规则命中回路数（规则库页右侧红点；含 0 命中规则）")
    @GetMapping("/alarmCountByRule")
    public Result<List<Map<String, Object>>> alarmCountByRule() {
        List<LightingDiagRule> rules = diagRuleService.listEnabled(null);
        List<Map<String, Object>> result = new ArrayList<>();
        if (rules == null || rules.isEmpty()) {
            return Result.ok(result);
        }
        Map<String, Long> countByRule = alarmService.countByRule();
        for (LightingDiagRule rule : rules) {
            Map<String, Object> item = new HashMap<>();
            item.put("ruleCode", rule.getRuleCode());
            item.put("ruleName", rule.getName());
            item.put("ruleDomain", rule.getRuleDomain());
            item.put("ruleLevel", rule.getRuleLevel());
            item.put("count", countByRule.getOrDefault(rule.getRuleCode(), 0L));
            result.add(item);
        }
        return Result.ok(result);
    }
}
