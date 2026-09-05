package org.jeecg.modules.bems.lighting.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.collection.CollectionUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingDiagRule;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDiagRuleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 照明回路智能诊断定时任务（R1 开路·灯具失效）
 * <p>
 * 判定口径（与规则表 R1 一致）：回路处于开启(status=开启) 且 实时电流低于额定电流，
 * 连续 3 个周期（每 1 分钟一拍，约 3 分钟）→ 标记该回路为"报警"，并在 lighting_circuit 记录命中规则信息。
 * 恢复逻辑：已报警回路连续 3 个周期不再满足条件 → 恢复为"正常"并清空命中规则信息。
 * <p>
 * 去抖状态放在进程内存（ConcurrentHashMap），重启后归零、需重新累积。
 * 范围：仅扫描 903（北区公区）空间下区域对应的回路。
 */
@Component
@AllArgsConstructor
@Slf4j
public class LightingDiagJob {

    /** 北区公区空间编码 */
    private static final String SPACE_BQ = "903";

    /** R1 规则编码 */
    private static final String RULE_CODE_R1 = "R1";

    /** R1 默认规则名称（规则表缺失时兜底） */
    private static final String RULE_NAME_R1_DEFAULT = "开路·灯具失效";

    /** 连续命中/连续恢复达到该次数即标记/恢复（每 1 分钟一拍，3 次≈3 分钟） */
    private static final int CONFIRM_COUNT = 3;

    private final ILightingCircuitService circuitService;

    private final ILightingAreaService areaService;

    private final ILightingDiagRuleService diagRuleService;

    /** 连续命中(异常)计数：circuitId -> 连续命中次数 */
    private final Map<Long, Integer> hitCounter = new ConcurrentHashMap<>();

    /** 连续恢复(正常)计数：circuitId -> 连续恢复次数 */
    private final Map<Long, Integer> recoverCounter = new ConcurrentHashMap<>();

    @Scheduled(cron = "0 * * * * ?")
    public void runR1Diagnose() {
        try {
            scanBqOpenCircuits();
        } catch (Exception e) {
            log.error("【诊断】R1 开路诊断定时任务异常", e);
        }
    }

    /**
     * 扫描 903 空间下所有区域对应的回路，执行 R1 开路判定
     */
    private void scanBqOpenCircuits() {
        // 903 空间下的所有区域
        List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getSpace, SPACE_BQ));
        if (CollectionUtil.isEmpty(areas)) {
            clearCounters();
            return;
        }
        Set<Long> areaIds = areas.stream().map(LightingArea::getId).collect(Collectors.toSet());
        // 这些区域下的全部回路
        List<LightingCircuit> circuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                .in(LightingCircuit::getAreaId, areaIds));
        if (CollectionUtil.isEmpty(circuits)) {
            clearCounters();
            return;
        }

        LightingDiagRule r1 = diagRuleService.getEnabledByCode(RULE_CODE_R1);
        // 规则停用则不执行 R1 判定
        if (r1 == null) {
            clearCounters();
            return;
        }
        String ruleName = r1.getName() != null ? r1.getName() : RULE_NAME_R1_DEFAULT;
        LocalDateTime now = LocalDateTime.now();

        int hitAlarm = 0;   // 本轮新标记报警数
        int hitRecover = 0; // 本轮恢复数

        for (LightingCircuit circuit : circuits) {
            Long circuitId = circuit.getId();
            boolean abnormal = isR1Abnormal(circuit);

            if (abnormal) {
                // 异常：累计命中次数
                hitCounter.merge(circuitId, 1, Integer::sum);
                recoverCounter.remove(circuitId);
                int count = hitCounter.getOrDefault(circuitId, 0);
                // 未报警且达到确认次数才标记；已报警的回路不再重复置位
                if (count >= CONFIRM_COUNT && !isAlarm(circuit)) {
                    markAlarm(circuit, ruleName, now);
                    hitAlarm++;
                }
            } else {
                // 正常：清零命中，累计恢复次数
                hitCounter.remove(circuitId);
                if (isAlarm(circuit)) {
                    recoverCounter.merge(circuitId, 1, Integer::sum);
                    int count = recoverCounter.getOrDefault(circuitId, 0);
                    if (count >= CONFIRM_COUNT) {
                        clearAlarm(circuit);
                        recoverCounter.remove(circuitId);
                        hitRecover++;
                    }
                } else {
                    recoverCounter.remove(circuitId);
                }
            }
        }

        // 清理不在本轮扫描范围的陈旧计数（例如回路被删除/移出 903）
        Set<Long> scannedIds = circuits.stream().map(LightingCircuit::getId).collect(Collectors.toSet());
        hitCounter.keySet().removeIf(id -> !scannedIds.contains(id));
        recoverCounter.keySet().removeIf(id -> !scannedIds.contains(id));

        if (hitAlarm > 0 || hitRecover > 0) {
            log.info("【诊断】R1 开路诊断执行完毕：新增报警 {} 条，恢复 {} 条", hitAlarm, hitRecover);
        }
    }

    /**
     * R1 异常判定：status=开启 且 有电流与额定电流 且 电流 < 额定电流
     */
    private boolean isR1Abnormal(LightingCircuit circuit) {
        if (!LightingCircuit.STATUS_ON.equals(circuit.getStatus())) {
            return false;
        }
        Double current = circuit.getElectricCurrent();
        Double rated = circuit.getRatedElectricCurrent();
        if (current == null || rated == null || rated <= 0) {
            // 额定电流为空/非法时不参与判定（避免把缺数据的正常回路误报）
            return false;
        }
        return current < rated;
    }

    private boolean isAlarm(LightingCircuit circuit) {
        return "报警".equals(circuit.getAlarmFlag());
    }

    private void markAlarm(LightingCircuit circuit, String ruleName, LocalDateTime now) {
        LightingCircuit update = new LightingCircuit();
        update.setId(circuit.getId());
        update.setAlarmFlag("报警");
        update.setAlarmRuleCode(RULE_CODE_R1);
        update.setAlarmRuleName(ruleName);
        update.setAlarmTime(now);
        update.setAlarmDetail(String.format(
                "开启状态但电流 %.2f A 低于额定电流 %.2f A，持续约 3 分钟（R1 开路·灯具失效）",
                circuit.getElectricCurrent() == null ? 0d : circuit.getElectricCurrent(),
                circuit.getRatedElectricCurrent() == null ? 0d : circuit.getRatedElectricCurrent()));
        circuitService.updateById(update);
        log.warn("【诊断】回路标记报警：circuitId={}, code={}, name={}, 电流={}A, 额定={}A",
                circuit.getId(), circuit.getCircuitCode(), circuit.getCircuitName(),
                circuit.getElectricCurrent(), circuit.getRatedElectricCurrent());
    }

    private void clearAlarm(LightingCircuit circuit) {
        LightingCircuit update = new LightingCircuit();
        update.setId(circuit.getId());
        update.setAlarmFlag("正常");
        update.setAlarmRuleCode(null);
        update.setAlarmRuleName(null);
        update.setAlarmTime(null);
        update.setAlarmDetail(null);
        circuitService.updateById(update);
        log.info("【诊断】回路恢复正常：circuitId={}, code={}", circuit.getId(), circuit.getCircuitCode());
    }

    private void clearCounters() {
        hitCounter.clear();
        recoverCounter.clear();
    }
}
