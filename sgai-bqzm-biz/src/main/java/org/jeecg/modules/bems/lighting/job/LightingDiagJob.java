package org.jeecg.modules.bems.lighting.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.collection.CollectionUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetry;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitAlarmLog;
import org.jeecg.modules.bems.lighting.entity.LightingDiagRule;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingBoxTelemetryService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitAlarmLogService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDiagRuleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 照明回路智能诊断定时任务（R1 开路·灯具失效 / R2 触点粘连·漏电 / R4 过载）
 * <p>
 * 判定口径（与规则表一致）：
 * <ul>
 *   <li>R1(开路)：回路 status=开启 且 电流接近零(&lt; 0.5A)，连续 3 拍(约3分钟)→报警。</li>
 *   <li>R2(粘连/漏电)：回路 status=关闭 且 残留电流 &gt; 0.2A，连续 3 拍(约3分钟)→报警。</li>
 *   <li>R4(过载)：回路 status=开启 且 电流 &gt; 额定电流，连续 3 拍(约3分钟)→报警。</li>
 * </ul>
 * 开启态同一回路只会命中 R1 或 R4 之一（开路/过载/正常互斥），按 R1→R4→正常 优先级判定。
 * 恢复逻辑：已报警回路连续 3 拍不再满足对应规则 → 恢复为"正常"并清空命中规则信息。
 * <p>
 * 前置排除：开启态判定前先看所属箱子三相电压是否正常，箱级电压异常(上级失电)则该箱回路本轮跳过，
 * 避免"整箱断电→所有开启回路开而无流"被连片误报成单回路开路。R2 不需要电压排除。
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

    /** R1/R2/R4 规则编码 */
    private static final String RULE_CODE_R1 = "R1";
    private static final String RULE_CODE_R2 = "R2";
    private static final String RULE_CODE_R4 = "R4";

    /** R1/R2/R4 默认规则名称（规则表缺失时兜底） */
    private static final String RULE_NAME_R1_DEFAULT = "开路·灯具失效";
    private static final String RULE_NAME_R2_DEFAULT = "触点粘连·漏电";
    private static final String RULE_NAME_R4_DEFAULT = "过载";

    /** R1 开路阈值(A)：开启状态电流低于此值(接近零)判定开路/不亮 */
    private static final double R1_OPEN_CURRENT = 0.5;

    /** R2 残留电流阈值(A)：关闭状态下电流高于此值判定粘连/漏电 */
    private static final double R2_STUCK_CURRENT = 0.2;

    /** 连续命中/连续恢复达到该次数即标记/恢复（每 1 分钟一拍，3 次≈3 分钟） */
    private static final int CONFIRM_COUNT = 3;

    /** 箱级三相电压有效下限/上限(V)：相电压低于下限视为该相失电/缺相，高于上限视为越限 */
    private static final double VOLTAGE_LOW = 180.0;
    private static final double VOLTAGE_HIGH = 253.0;

    private final ILightingCircuitService circuitService;

    private final ILightingAreaService areaService;

    private final ILightingDiagRuleService diagRuleService;

    private final ILightingBoxTelemetryService boxTelemetryService;

    private final ILightingCircuitAlarmLogService alarmLogService;

    /** R1 去抖计数：circuitId -> 连续命中/连续恢复次数 */
    private final Map<Long, Integer> r1Hit = new ConcurrentHashMap<>();
    private final Map<Long, Integer> r1Recover = new ConcurrentHashMap<>();

    /** R2 去抖计数：circuitId -> 连续命中/连续恢复次数 */
    private final Map<Long, Integer> r2Hit = new ConcurrentHashMap<>();
    private final Map<Long, Integer> r2Recover = new ConcurrentHashMap<>();

    /** R4 去抖计数：circuitId -> 连续命中/连续恢复次数 */
    private final Map<Long, Integer> r4Hit = new ConcurrentHashMap<>();
    private final Map<Long, Integer> r4Recover = new ConcurrentHashMap<>();

    @Scheduled(cron = "0 * * * * ?")
    public void runDiagnose() {
        try {
            scanBqDiagnose();
        } catch (Exception e) {
            log.error("【诊断】开路/粘连诊断定时任务异常", e);
        }
    }

    /**
     * 扫描 903 空间下所有区域对应的回路，按开关状态路由到 R1(开启) / R2(关闭) 判定
     */
    private void scanBqDiagnose() {
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
        LightingDiagRule r2 = diagRuleService.getEnabledByCode(RULE_CODE_R2);
        LightingDiagRule r4 = diagRuleService.getEnabledByCode(RULE_CODE_R4);
        String r1Name = r1 != null && r1.getName() != null ? r1.getName() : RULE_NAME_R1_DEFAULT;
        String r2Name = r2 != null && r2.getName() != null ? r2.getName() : RULE_NAME_R2_DEFAULT;
        String r4Name = r4 != null && r4.getName() != null ? r4.getName() : RULE_NAME_R4_DEFAULT;
        LocalDateTime now = LocalDateTime.now();

        // 箱级三相电压快照：areaId -> 箱子遥测，仅 R1(开启态)判定用于前置排除上级失电
        Map<Long, LightingBoxTelemetry> boxMap = loadBoxVoltageMap(areaIds);
        // 区域映射：areaId -> area，用于报警流水回填 区域名/空间
        Map<Long, LightingArea> areaMap = areas.stream()
                .collect(Collectors.toMap(LightingArea::getId, Function.identity(), (a, b) -> a));

        int hitAlarm = 0;   // 本轮新标记报警数
        int hitRecover = 0; // 本轮恢复数
        int skipped = 0;    // 本轮被跳过的回路数（电压异常/未知状态）

        for (LightingCircuit circuit : circuits) {
            Long circuitId = circuit.getId();
            LightingArea area = areaMap.get(circuit.getAreaId());
            String status = circuit.getStatus();
            if (LightingCircuit.STATUS_ON.equals(status)) {
                // —— 开启态：可命中 R1(开路) 或 R4(过载)，需先做箱级三相电压前置排除 ——
                LightingBoxTelemetry box = boxMap.get(circuit.getAreaId());
                if (!isBoxVoltageOk(box)) {
                    // 箱级电压异常(上级失电)：本箱开启回路无法判定开路/过载，跳过
                    r1Hit.remove(circuitId);
                    r1Recover.remove(circuitId);
                    r4Hit.remove(circuitId);
                    r4Recover.remove(circuitId);
                    skipped++;
                    continue;
                }
                int marked;
                if (isR1Abnormal(circuit)) {
                    marked = handleCircuit(circuit, area, now, true,
                            r1Hit, r1Recover, RULE_CODE_R1, r1Name, buildR1Detail(circuit));
                } else if (isR4Abnormal(circuit)) {
                    marked = handleCircuit(circuit, area, now, true,
                            r4Hit, r4Recover, RULE_CODE_R4, r4Name, buildR4Detail(circuit));
                } else {
                    // 开启态无异常：R1/R4 都恢复；按当前命中规则走恢复去抖
                    marked = handleOnNormal(circuit, area, now);
                }
                if (marked > 0) {
                    hitAlarm++;
                } else if (marked < 0) {
                    hitRecover++;
                }
            } else if (LightingCircuit.STATUS_OFF.equals(status)) {
                // —— 关闭态：走 R2，无需电压排除 ——
                boolean abnormal = isR2Abnormal(circuit);
                int marked = handleCircuit(circuit, area, now, abnormal,
                        r2Hit, r2Recover, RULE_CODE_R2, r2Name,
                        buildR2Detail(circuit));
                if (marked > 0) {
                    hitAlarm++;
                } else if (marked < 0) {
                    hitRecover++;
                }
            } else {
                // 未知/其它状态：不参与判定，清空相关去抖计数
                r1Hit.remove(circuitId);
                r1Recover.remove(circuitId);
                r2Hit.remove(circuitId);
                r2Recover.remove(circuitId);
                r4Hit.remove(circuitId);
                r4Recover.remove(circuitId);
                skipped++;
            }
        }

        // 清理不在本轮扫描范围的陈旧计数（例如回路被删除/移出 903）
        Set<Long> scannedIds = circuits.stream().map(LightingCircuit::getId).collect(Collectors.toSet());
        pruneCounters(scannedIds);

        if (hitAlarm > 0 || hitRecover > 0 || skipped > 0) {
            log.info("【诊断】开路/粘连诊断执行完毕：新增报警 {} 条，恢复 {} 条，跳过 {} 回路",
                    hitAlarm, hitRecover, skipped);
        }
    }

    /**
     * 通用去抖处理。
     *
     * @return 1=本轮新标记报警；-1=本轮恢复；0=无变化
     */
    private int handleCircuit(LightingCircuit circuit, LightingArea area, LocalDateTime now, boolean abnormal,
                              Map<Long, Integer> hitCounter, Map<Long, Integer> recoverCounter,
                              String ruleCode, String ruleName, String alarmDetail) {
        Long circuitId = circuit.getId();
        if (abnormal) {
            // 异常：累计命中
            hitCounter.merge(circuitId, 1, Integer::sum);
            recoverCounter.remove(circuitId);
            int count = hitCounter.getOrDefault(circuitId, 0);
            // 未报警且达到确认次数才标记；已报警的不再重复置位
            if (count >= CONFIRM_COUNT && !isAlarm(circuit)) {
                markAlarm(circuit, area, now, ruleCode, ruleName, alarmDetail);
                return 1;
            }
        } else {
            // 正常：清零命中，累计恢复
            hitCounter.remove(circuitId);
            if (isAlarm(circuit)) {
                recoverCounter.merge(circuitId, 1, Integer::sum);
                int count = recoverCounter.getOrDefault(circuitId, 0);
                if (count >= CONFIRM_COUNT) {
                    clearAlarm(circuit, now);
                    recoverCounter.remove(circuitId);
                    return -1;
                }
            } else {
                recoverCounter.remove(circuitId);
            }
        }
        return 0;
    }

    /**
     * 开启态回路处于正常范围(既非开路也非过载)时的恢复处理。
     * R1/R4 共用当前回路的一条报警记录；按当前命中的规则走对应恢复去抖。
     *
     * @return 1=标记报警(理论不发生)；-1=恢复；0=无变化
     */
    private int handleOnNormal(LightingCircuit circuit, LightingArea area, LocalDateTime now) {
        Long circuitId = circuit.getId();
        // 正常：R1/R4 命中计数都清零
        r1Hit.remove(circuitId);
        r4Hit.remove(circuitId);
        if (isAlarm(circuit)) {
            // 当前命中规则决定用哪套恢复计数
            String ruleCode = circuit.getAlarmRuleCode();
            Map<Long, Integer> recoverCounter =
                    RULE_CODE_R4.equals(ruleCode) ? r4Recover : r1Recover;
            recoverCounter.merge(circuitId, 1, Integer::sum);
            if (RULE_CODE_R4.equals(ruleCode)) {
                r1Recover.remove(circuitId);
            } else {
                r4Recover.remove(circuitId);
            }
            int count = recoverCounter.getOrDefault(circuitId, 0);
            if (count >= CONFIRM_COUNT) {
                clearAlarm(circuit, now);
                recoverCounter.remove(circuitId);
                return -1;
            }
        } else {
            r1Recover.remove(circuitId);
            r4Recover.remove(circuitId);
        }
        return 0;
    }

    /**
     * R1 异常判定：开启状态电流接近零(低于 0.5A)判定开路/不亮，与额定电流无关
     */
    private boolean isR1Abnormal(LightingCircuit circuit) {
        Double current = circuit.getElectricCurrent();
        return current != null && current < R1_OPEN_CURRENT;
    }

    /**
     * R4 异常判定：开启状态电流超过该回路额定电流，判定过载
     */
    private boolean isR4Abnormal(LightingCircuit circuit) {
        Double current = circuit.getElectricCurrent();
        Double rated = circuit.getRatedElectricCurrent();
        if (current == null || rated == null || rated <= 0) {
            // 额定电流为空/非法时不参与过载判定
            return false;
        }
        return current > rated;
    }

    /**
     * R2 异常判定：status=关闭 且 残留电流 > 0.2A（触点粘连/漏电，关不断）
     */
    private boolean isR2Abnormal(LightingCircuit circuit) {
        Double current = circuit.getElectricCurrent();
        return current != null && current > R2_STUCK_CURRENT;
    }

    private String buildR1Detail(LightingCircuit circuit) {
        return String.format(
                "开启状态但电流 %.2f A 低于开路阈值 %.2f A(接近零)，持续约 3 分钟，疑似开路/灯具失效（R1）",
                circuit.getElectricCurrent() == null ? 0d : circuit.getElectricCurrent(),
                R1_OPEN_CURRENT);
    }

    private String buildR2Detail(LightingCircuit circuit) {
        return String.format(
                "关闭状态但残留电流 %.2f A 超过阈值 %.2f A，持续约 3 分钟，疑似触点粘连/漏电（R2）",
                circuit.getElectricCurrent() == null ? 0d : circuit.getElectricCurrent(),
                R2_STUCK_CURRENT);
    }

    private String buildR4Detail(LightingCircuit circuit) {
        return String.format(
                "开启状态但电流 %.2f A 超过额定电流 %.2f A，持续约 3 分钟，疑似过载（R4）",
                circuit.getElectricCurrent() == null ? 0d : circuit.getElectricCurrent(),
                circuit.getRatedElectricCurrent() == null ? 0d : circuit.getRatedElectricCurrent());
    }

    /**
     * 加载各区域的箱级三相电压快照：areaId -> 箱子遥测
     */
    private Map<Long, LightingBoxTelemetry> loadBoxVoltageMap(Set<Long> areaIds) {
        if (CollectionUtil.isEmpty(areaIds)) {
            return Collections.emptyMap();
        }
        List<LightingBoxTelemetry> boxes = boxTelemetryService.list(
                new LambdaQueryWrapper<LightingBoxTelemetry>().in(LightingBoxTelemetry::getAreaId, areaIds));
        if (CollectionUtil.isEmpty(boxes)) {
            return Collections.emptyMap();
        }
        return boxes.stream().collect(Collectors.toMap(LightingBoxTelemetry::getAreaId,
                b -> b, (a, b) -> a));
    }

    /**
     * 箱级三相电压是否正常（上级供电正常）：A/B/C 三相均有值且落在有效带内。
     * 任一相为 null/缺相(0或过低)/过高 都视为箱级供电异常。
     */
    private boolean isBoxVoltageOk(LightingBoxTelemetry box) {
        if (box == null) {
            return false;
        }
        return inVoltageBand(box.getVoltageA())
                && inVoltageBand(box.getVoltageB())
                && inVoltageBand(box.getVoltageC());
    }

    private boolean inVoltageBand(Double v) {
        return v != null && v >= VOLTAGE_LOW && v <= VOLTAGE_HIGH;
    }

    private boolean isAlarm(LightingCircuit circuit) {
        return "报警".equals(circuit.getAlarmFlag());
    }

    private void markAlarm(LightingCircuit circuit, LightingArea area, LocalDateTime now,
                           String ruleCode, String ruleName, String alarmDetail) {
        LightingCircuit update = new LightingCircuit();
        update.setId(circuit.getId());
        update.setAlarmFlag("报警");
        update.setAlarmRuleCode(ruleCode);
        update.setAlarmRuleName(ruleName);
        update.setAlarmTime(now);
        update.setAlarmDetail(alarmDetail);
        circuitService.updateById(update);
        log.warn("【诊断】回路标记报警：circuitId={}, code={}, name={}, 规则={}({}), 电流={}A",
                circuit.getId(), circuit.getCircuitCode(), circuit.getCircuitName(),
                ruleCode, ruleName, circuit.getElectricCurrent());

        // 写入报警历史流水（一次报警生命周期开始）
        LightingCircuitAlarmLog alarmLog = new LightingCircuitAlarmLog();
        alarmLog.setCircuitId(circuit.getId());
        alarmLog.setCircuitCode(circuit.getCircuitCode());
        alarmLog.setCircuitName(circuit.getCircuitName());
        alarmLog.setAreaId(circuit.getAreaId());
        alarmLog.setAreaName(area != null ? area.getAreaName() : null);
        alarmLog.setSpace(area != null ? area.getSpace() : SPACE_BQ);
        alarmLog.setRuleCode(ruleCode);
        alarmLog.setRuleName(ruleName);
        alarmLog.setStatus("报警中");
        alarmLog.setAlarmTime(now);
        alarmLog.setElectricCurrent(toBigDecimal(circuit.getElectricCurrent()));
        alarmLog.setRatedElectricCurrent(toBigDecimal(circuit.getRatedElectricCurrent()));
        alarmLog.setDetail(alarmDetail);
        alarmLogService.logAlarm(alarmLog);
    }

    private void clearAlarm(LightingCircuit circuit, LocalDateTime now) {
        LightingCircuit update = new LightingCircuit();
        update.setId(circuit.getId());
        update.setAlarmFlag("正常");
        update.setAlarmRuleCode(null);
        update.setAlarmRuleName(null);
        update.setAlarmTime(null);
        update.setAlarmDetail(null);
        circuitService.updateById(update);
        log.info("【诊断】回路恢复正常：circuitId={}, code={}", circuit.getId(), circuit.getCircuitCode());

        // 关闭最近一条"报警中"流水：回填恢复时间并置为已恢复
        alarmLogService.recoverAlarm(circuit.getId(), now);
    }

    private java.math.BigDecimal toBigDecimal(Double v) {
        return v == null ? null : java.math.BigDecimal.valueOf(v);
    }

    private void pruneCounters(Set<Long> scannedIds) {
        r1Hit.keySet().removeIf(id -> !scannedIds.contains(id));
        r1Recover.keySet().removeIf(id -> !scannedIds.contains(id));
        r2Hit.keySet().removeIf(id -> !scannedIds.contains(id));
        r2Recover.keySet().removeIf(id -> !scannedIds.contains(id));
        r4Hit.keySet().removeIf(id -> !scannedIds.contains(id));
        r4Recover.keySet().removeIf(id -> !scannedIds.contains(id));
    }

    private void clearCounters() {
        r1Hit.clear();
        r1Recover.clear();
        r2Hit.clear();
        r2Recover.clear();
        r4Hit.clear();
        r4Recover.clear();
    }
}
