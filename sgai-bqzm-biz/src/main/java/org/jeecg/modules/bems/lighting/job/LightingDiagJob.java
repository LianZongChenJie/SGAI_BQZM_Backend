package org.jeecg.modules.bems.lighting.job;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetry;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitAlarm;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitAlarmLog;
import org.jeecg.modules.bems.lighting.entity.LightingDiagRule;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingBoxTelemetryService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitAlarmLogService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitAlarmService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDiagRuleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 照明回路智能诊断定时任务（R1 开路 / R2 粘连·漏电 / R3 电压越限 / R4 过载）
 * <p>
 * 判定口径（与规则表一致，命中后统一写入"当前命中明细表 lighting_circuit_alarm"，一行=回路×规则）：
 * <ul>
 *   <li>R1(开路,alarm)：开启 且 电流 &lt; 0.5A（接近零），3 拍确认。</li>
 *   <li>R2(粘连/漏电,alarm)：关闭 且 残留电流 &gt; 0.2A，3 拍确认。</li>
 *   <li>R3(电压越限,warn)：任一相电压出带(198~242V) → 该箱下每回路各记一条，1 拍确认。</li>
 *   <li>R4(过载,alarm)：开启 且 电流 &gt; 额定电流，3 拍确认。</li>
 *   <li>R5C(通信中断,alarm)：某箱所有回路 comstat=离线 → 箱级通信中断，冗余到该箱每回路；此时冻结数据，暂停该箱 R1/R2/R3/R4 判定。</li>
 * </ul>
 * 同一回路可同时命中多条规则（如 R1+R3 / R4+R3），各级别并存；因此采用命中表而非单列表达。
 * R1/R4 仅在本箱三相供电基本正常(180~253V，排除上级失电)时判定；R3 只由箱电压本身决定；R2 不依赖电压；
 * R5C 按箱 comstat 判定并压制同箱其它电气规则（冻结值不参与判定）。
 * <p>
 * 范围：仅扫描 903（北区公区）空间。历史追溯仍写 lighting_circuit_alarm_log（一次报警生命周期）。
 */
@Component
@AllArgsConstructor
@Slf4j
public class LightingDiagJob {

    /** 北区公区空间编码 */
    private static final String SPACE_BQ = "903";

    /** 规则编码 */
    private static final String RULE_R1 = "R1";
    private static final String RULE_R2 = "R2";
    private static final String RULE_R3 = "R3";
    private static final String RULE_R4 = "R4";
    private static final String RULE_R5C = "R5C";

    /** R1 开路阈值(A)：开启态电流低于此(接近零)判开路 */
    private static final double R1_OPEN_CURRENT = 0.5;
    /** R2 残留电流阈值(A)：关闭态电流高于此判粘连/漏电 */
    private static final double R2_STUCK_CURRENT = 0.2;

    /** R1/R4 供电基本正常带(排除上级失电) */
    private static final double VOLTAGE_LOW = 180.0;
    private static final double VOLTAGE_HIGH = 253.0;
    /** R3 电压越限带：任一相超出即欠压/过压 */
    private static final double R3_V_LO = 198.0;
    private static final double R3_V_HI = 242.0;

    /** 每规则确认拍数：R3(电压)1 拍，其余 3 拍 */
    private static final Map<String, Integer> CONFIRM = new HashMap<>();
    static {
        CONFIRM.put(RULE_R1, 3);
        CONFIRM.put(RULE_R2, 3);
        CONFIRM.put(RULE_R3, 1);
        CONFIRM.put(RULE_R4, 3);
        CONFIRM.put(RULE_R5C, 3);
    }

    private final ILightingCircuitService circuitService;
    private final ILightingAreaService areaService;
    private final ILightingDiagRuleService diagRuleService;
    private final ILightingBoxTelemetryService boxTelemetryService;
    private final ILightingCircuitAlarmService alarmService;
    private final ILightingCircuitAlarmLogService alarmLogService;

    /** 去抖缓冲：circuitId -> (ruleCode -> 连续命中拍数) */
    private final Map<Long, Map<String, Integer>> hitBuf = new ConcurrentHashMap<>();
    /** 上一轮最终命中 key（circuitId:ruleCode），用于驱动历史流水 diff */
    private final Set<String> prevFinalKeys = ConcurrentHashMap.newKeySet();

    @Scheduled(cron = "0 * * * * ?")
    public void runDiagnose() {
        try {
            scanBqDiagnose();
        } catch (Exception e) {
            log.error("【诊断】开路/粘连/电压/过载诊断定时任务异常", e);
        }
    }

    private void scanBqDiagnose() {
        List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getSpace, SPACE_BQ));
        if (CollectionUtil.isEmpty(areas)) {
            clearAll();
            return;
        }
        Set<Long> areaIds = areas.stream().map(LightingArea::getId).collect(Collectors.toSet());
        Map<Long, LightingArea> areaMap = areas.stream()
                .collect(Collectors.toMap(LightingArea::getId, Function.identity(), (a, b) -> a));
        List<LightingCircuit> circuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                .in(LightingCircuit::getAreaId, areaIds));
        if (CollectionUtil.isEmpty(circuits)) {
            clearAll();
            return;
        }
        // 箱电压快照 areaId -> box
        Map<Long, LightingBoxTelemetry> boxMap = loadBoxVoltageMap(areaIds);

        // 整箱通信中断判定：某区域有回路且所有回路 comstat=离线 → 判为箱级通信中断
        Set<Long> commDownAreaIds = computeCommDownAreas(circuits);

        // 规则元数据：ruleCode -> (name, level)；缺失时按默认名称+级别兜底
        Map<String, RuleMeta> ruleMeta = loadRuleMeta();

        LocalDateTime now = LocalDateTime.now();

        // 本轮"最终命中"（已过确认拍数）
        Map<Long, List<LightingCircuitAlarm>> finalHits = new HashMap<>();

        for (LightingCircuit circuit : circuits) {
            Long cid = circuit.getId();
            LightingArea area = areaMap.get(circuit.getAreaId());
            LightingBoxTelemetry box = boxMap.get(circuit.getAreaId());

            // 1) 计算本轮候选命中规则
            List<String> candidates = computeCandidates(circuit, area, box, commDownAreaIds, ruleMeta);
            // 2) 去抖：更新 hitBuf
            Map<String, Integer> buf = hitBuf.computeIfAbsent(cid, k -> new ConcurrentHashMap<>());
            Set<String> candSet = new HashSet<>(candidates);
            buf.keySet().removeIf(code -> !candSet.contains(code)); // 候选外清零
            for (String code : candidates) {
                int cnt = buf.merge(code, 1, Integer::sum);
                int need = CONFIRM.getOrDefault(code, 3);
                if (cnt >= need) {
                    finalHits.computeIfAbsent(cid, k -> new ArrayList<>()).add(buildAlarmHit(circuit, area, code, ruleMeta, now));
                }
            }
        }

        // 3) 汇总本轮最终命中行
        List<LightingCircuitAlarm> newHitsRows = new ArrayList<>();
        for (List<LightingCircuitAlarm> rows : finalHits.values()) {
            newHitsRows.addAll(rows);
        }
        Set<String> currentKeys = new HashSet<>();
        for (Map.Entry<Long, List<LightingCircuitAlarm>> e : finalHits.entrySet()) {
            for (LightingCircuitAlarm a : e.getValue()) {
                currentKeys.add(keyOf(e.getKey(), a.getRuleCode()));
            }
        }

        // 4) 重建命中表（903 全量替换）
        alarmService.rebuildBySpace(SPACE_BQ, newHitsRows);

        // 5) 历史流水 diff：
        //    新增命中（本轮有、上轮无）→ 写一条"报警中"开始；消失（上轮有、本轮无）→ 回填恢复
        Set<String> goneKeys = new HashSet<>(prevFinalKeys);
        goneKeys.removeAll(currentKeys);
        Set<String> newKeys = new HashSet<>(currentKeys);
        newKeys.removeAll(prevFinalKeys);

        List<LightingCircuitAlarm> allHits = finalHits.values().stream()
                .flatMap(List::stream).collect(Collectors.toList());
        Map<String, LightingCircuitAlarm> hitByKey = new HashMap<>();
        for (LightingCircuitAlarm a : allHits) {
            hitByKey.put(keyOf(a.getCircuitId(), a.getRuleCode()), a);
        }
        int logNew = 0;
        for (String k : newKeys) {
            LightingCircuitAlarm a = hitByKey.get(k);
            if (a != null && !hasActiveLog(a.getCircuitId(), a.getRuleCode())) {
                writeLogStart(a);
                logNew++;
            }
        }
        int logRecover = 0;
        for (String k : goneKeys) {
            // 用上一轮该(回路,规则)的 alarmTime 作为开始时间不太必要，恢复用本轮时间即可
            String[] p = k.split(":");
            try {
                if (p.length == 2 && alarmLogService.recoverAlarm(Long.parseLong(p[0]), p[1], now)) {
                    logRecover++;
                }
            } catch (Exception ignore) {
                // ignore malformed key
            }
        }

        // 6) 更新上轮最终命中 & 清理 hitBuf 陈旧
        prevFinalKeys.clear();
        prevFinalKeys.addAll(currentKeys);
        Set<Long> scannedIds = circuits.stream().map(LightingCircuit::getId).collect(Collectors.toSet());
        hitBuf.keySet().removeIf(id -> !scannedIds.contains(id));

        log.info("【诊断】903 诊断完成：当前命中 {} 条(规则x回路)，历史新增 {} 条，恢复 {} 条",
                newHitsRows.size(), logNew, logRecover);
    }

    /**
     * 计算某回路本轮候选命中规则（未去抖）
     */
    private List<String> computeCandidates(LightingCircuit circuit, LightingArea area,
                                           LightingBoxTelemetry box, Set<Long> commDownAreaIds,
                                           Map<String, RuleMeta> ruleMeta) {
        List<String> hits = new ArrayList<>();
        if (ruleMeta.isEmpty()) {
            return hits;
        }
        Long areaId = area != null ? area.getId() : circuit.getAreaId();

        // 整箱通信中断：该箱所有回路离线 → 只报 R5C，冻结数据暂停其它电气判定
        if (areaId != null && commDownAreaIds.contains(areaId)) {
            hits.add(RULE_R5C);
            return hits;
        }

        // 单回路离线(非整箱中断)：冻结数据不参与 R1/R2/R4 电气判定；R3(箱电压)仍可判
        boolean ownOffline = LightingCircuit.COMSTAT_OFFLINE.equals(circuit.getComstat());

        // R3 电压越限：箱电压任一相出带(有遥测)。冗余到该回路。
        if (box != null && !allInVoltageBand(box, R3_V_LO, R3_V_HI)) {
            hits.add(RULE_R3);
        }
        if (ownOffline) {
            // 离线回路的电流为冻结值，不判 R1/R2/R4
            return hits;
        }

        String status = circuit.getStatus();
        boolean on = LightingCircuit.STATUS_ON.equals(status);
        boolean off = LightingCircuit.STATUS_OFF.equals(status);
        Double cur = circuit.getElectricCurrent();
        if (on && isPowerNormal(box)) {
            // 供电基本正常才判 R1/R4
            if (cur != null && cur < R1_OPEN_CURRENT) {
                hits.add(RULE_R1);
            } else {
                Double rated = circuit.getRatedElectricCurrent();
                if (cur != null && rated != null && rated > 0 && cur > rated) {
                    hits.add(RULE_R4);
                }
            }
        } else if (off) {
            // R2 不依赖箱电压
            if (cur != null && cur > R2_STUCK_CURRENT) {
                hits.add(RULE_R2);
            }
        }
        return hits;
    }

    private LightingCircuitAlarm buildAlarmHit(LightingCircuit c, LightingArea area, String code,
                                               Map<String, RuleMeta> ruleMeta, LocalDateTime now) {
        RuleMeta meta = ruleMeta.getOrDefault(code, new RuleMeta(code));
        LightingCircuitAlarm a = new LightingCircuitAlarm();
        a.setCircuitId(c.getId());
        a.setCircuitCode(c.getCircuitCode());
        a.setCircuitName(c.getCircuitName());
        a.setAreaId(c.getAreaId());
        a.setAreaName(area != null ? area.getAreaName() : null);
        a.setSpace(area != null ? area.getSpace() : SPACE_BQ);
        a.setRuleCode(code);
        a.setRuleName(meta.name);
        a.setRuleLevel(meta.level);
        a.setAlarmTime(now);
        return a;
    }

    private Map<String, RuleMeta> loadRuleMeta() {
        Map<String, RuleMeta> map = new HashMap<>();
        addRuleMeta(map, diagRuleService.getEnabledByCode(RULE_R1), RULE_R1, "开路·灯具失效", "alarm");
        addRuleMeta(map, diagRuleService.getEnabledByCode(RULE_R2), RULE_R2, "触点粘连·漏电", "alarm");
        addRuleMeta(map, diagRuleService.getEnabledByCode(RULE_R3), RULE_R3, "电压越限", "warn");
        addRuleMeta(map, diagRuleService.getEnabledByCode(RULE_R4), RULE_R4, "过载", "alarm");
        addRuleMeta(map, diagRuleService.getEnabledByCode(RULE_R5C), RULE_R5C, "通信中断", "alarm");
        return map;
    }

    private void addRuleMeta(Map<String, RuleMeta> map, LightingDiagRule r, String code, String defName, String defLevel) {
        if (r == null) {
            return;
        }
        map.put(code, new RuleMeta(
                r.getName() != null ? r.getName() : defName,
                r.getRuleLevel() != null ? r.getRuleLevel() : defLevel));
    }

    private static final class RuleMeta {
        String name;
        String level;
        RuleMeta(String code) {
            this.name = code;
            this.level = "alarm";
        }
        RuleMeta(String name, String level) {
            this.name = name;
            this.level = level;
        }
    }

    /**
     * 整箱通信中断判定：某区域有回路，且该区域所有回路 comstat=离线 → 视为箱级通信中断。
     */
    private Set<Long> computeCommDownAreas(List<LightingCircuit> circuits) {
        if (CollectionUtil.isEmpty(circuits)) {
            return java.util.Collections.emptySet();
        }
        Map<Long, List<LightingCircuit>> byArea = circuits.stream()
                .filter(c -> c.getAreaId() != null)
                .collect(Collectors.groupingBy(LightingCircuit::getAreaId));
        Set<Long> result = new HashSet<>();
        for (Map.Entry<Long, List<LightingCircuit>> e : byArea.entrySet()) {
            List<LightingCircuit> list = e.getValue();
            if (list.isEmpty()) {
                continue;
            }
            boolean allOffline = list.stream()
                    .allMatch(c -> LightingCircuit.COMSTAT_OFFLINE.equals(c.getComstat()));
            if (allOffline) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    private Map<Long, LightingBoxTelemetry> loadBoxVoltageMap(Set<Long> areaIds) {
        if (CollectionUtil.isEmpty(areaIds)) {
            return java.util.Collections.emptyMap();
        }
        List<LightingBoxTelemetry> boxes = boxTelemetryService.list(
                new LambdaQueryWrapper<LightingBoxTelemetry>().in(LightingBoxTelemetry::getAreaId, areaIds));
        if (CollectionUtil.isEmpty(boxes)) {
            return java.util.Collections.emptyMap();
        }
        return boxes.stream().collect(Collectors.toMap(LightingBoxTelemetry::getAreaId,
                Function.identity(), (a, b) -> a));
    }

    private boolean isPowerNormal(LightingBoxTelemetry box) {
        return box != null && allInVoltageBand(box, VOLTAGE_LOW, VOLTAGE_HIGH);
    }

    private boolean allInVoltageBand(LightingBoxTelemetry box, double lo, double hi) {
        if (box == null) {
            return false;
        }
        return inBand(box.getVoltageA(), lo, hi)
                && inBand(box.getVoltageB(), lo, hi)
                && inBand(box.getVoltageC(), lo, hi);
    }

    private boolean inBand(Double v, double lo, double hi) {
        return v != null && v >= lo && v <= hi;
    }

    private String keyOf(Long circuitId, String ruleCode) {
        return circuitId + ":" + ruleCode;
    }

    private boolean hasActiveLog(Long circuitId, String ruleCode) {
        return !alarmLogService.listActiveByCircuit(circuitId, ruleCode).isEmpty();
    }

    private void writeLogStart(LightingCircuitAlarm a) {
        LightingCircuitAlarmLog logRow = new LightingCircuitAlarmLog();
        logRow.setCircuitId(a.getCircuitId());
        logRow.setCircuitCode(a.getCircuitCode());
        logRow.setCircuitName(a.getCircuitName());
        logRow.setAreaId(a.getAreaId());
        logRow.setAreaName(a.getAreaName());
        logRow.setSpace(a.getSpace());
        logRow.setRuleCode(a.getRuleCode());
        logRow.setRuleName(a.getRuleName());
        logRow.setStatus("报警中");
        logRow.setAlarmTime(a.getAlarmTime());
        alarmLogService.logAlarm(logRow);
    }

    private void clearAll() {
        hitBuf.clear();
        prevFinalKeys.clear();
    }
}
