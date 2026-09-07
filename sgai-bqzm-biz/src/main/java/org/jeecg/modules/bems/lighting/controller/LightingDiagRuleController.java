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

    @ApiOperation("查询报警回路列表（全空间；含区域/电流/额定电流/命中规则）")
    @GetMapping("/alarmCircuits")
    public Result<List<LightingCircuit>> alarmCircuits() {
        // 全空间所有被标记为报警的回路
        List<LightingCircuit> circuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                .eq(LightingCircuit::getAlarmFlag, "报警"));
        if (circuits == null || circuits.isEmpty()) {
            return Result.ok(java.util.Collections.emptyList());
        }
        // 回填区域名称/空间名称（用到的区域一次性查）
        Set<Long> needAreaIds = circuits.stream()
                .map(LightingCircuit::getAreaId)
                .collect(Collectors.toSet());
        List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .in(LightingArea::getId, needAreaIds));
        Map<Long, LightingArea> areaMap = (areas == null ? java.util.Collections.<LightingArea>emptyList() : areas)
                .stream()
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

    @ApiOperation("报警回路总数（全空间）")
    @GetMapping("/alarmCount")
    public Result<Long> alarmCount() {
        Long count = circuitService.count(new LambdaQueryWrapper<LightingCircuit>()
                .eq(LightingCircuit::getAlarmFlag, "报警"));
        return Result.ok(count);
    }

    @ApiOperation("各规则命中回路数（全空间；规则库页右侧红点；含 0 命中规则）")
    @GetMapping("/alarmCountByRule")
    public Result<List<Map<String, Object>>> alarmCountByRule() {
        // 以启用规则为准，保证所有规则(含未命中=0)都返回，前端每条规则右侧都能显示
        List<LightingDiagRule> rules = diagRuleService.listEnabled(null);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        if (rules == null || rules.isEmpty()) {
            return Result.ok(result);
        }
        // 全空间所有报警回路，按 alarm_rule_code 分组统计；命中规则为空(null)不计
        List<LightingCircuit> alarmCircuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                .eq(LightingCircuit::getAlarmFlag, "报警"));
        Map<String, Long> countByRule = alarmCircuits.stream()
                .filter(c -> c.getAlarmRuleCode() != null)
                .collect(Collectors.groupingBy(LightingCircuit::getAlarmRuleCode, Collectors.counting()));
        for (LightingDiagRule rule : rules) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("ruleCode", rule.getRuleCode());
            item.put("ruleName", rule.getName());
            item.put("ruleDomain", rule.getRuleDomain());
            item.put("count", countByRule.getOrDefault(rule.getRuleCode(), 0L));
            result.add(item);
        }
        return Result.ok(result);
    }
}
