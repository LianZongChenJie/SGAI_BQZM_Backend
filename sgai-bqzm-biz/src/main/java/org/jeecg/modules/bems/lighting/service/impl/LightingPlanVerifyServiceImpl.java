package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.constant.BusinessConfigConstant;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecuteLog;
import org.jeecg.modules.bems.lighting.entity.LightingScene;
import org.jeecg.modules.bems.lighting.entity.LightingSceneDetail;
import org.jeecg.modules.bems.lighting.mapper.LightingSceneDetailMapper;
import org.jeecg.modules.bems.lighting.service.IBusinessConfigService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecuteLogService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanVerifyService;
import org.jeecg.modules.bems.lighting.service.ILightingSceneService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 计划"持续验证"服务实现
 * <p>
 * 存储复用执行日志表 lighting_plan_execute_log（无需新增表）：
 * 执行成功时把该行 verify_status 置"待验证"、verify_time=执行时刻+延迟；
 * 定时任务扫描到期记录，复查计划目标回路状态并对未达成的补下发，最后置"已完成"。
 */
@Slf4j
@Service
@AllArgsConstructor
public class LightingPlanVerifyServiceImpl implements ILightingPlanVerifyService {

    /** 全局开关值：只有配成这两个字才算开启 */
    private static final String SWITCH_ON = "开启";
    /** 默认延迟分钟数（业务配置缺失时兜底） */
    private static final int DEFAULT_DELAY_MINUTES = 3;
    /** 默认验证总轮次（业务配置缺失时兜底，1=只验一次） */
    private static final int DEFAULT_VERIFY_TIMES = 1;
    /** verify_result 列长度上限 */
    private static final int RESULT_MAX_LEN = 500;
    /** 补偿父日志的名称标识：控制日志列表"名称"列一眼可辨 */
    private static final String VERIFY_LOG_TAG = "持续验证补发";
    /** 复查通过日志的名称标识：本轮无回路需要补发时留痕，控制日志列表"名称"列一眼可辨 */
    private static final String VERIFY_PASS_TAG = "持续验证复查通过";
    /**
     * 复查通过日志的操作类型。
     * <p>
     * 必须用"复查通过"这类**不含"开/关"字样**的写法：控制日志输出层会按
     * {@code contains("关") / contains("开")} 把 operationType 简化成"关闭/开启"，
     * 若写成"复查通过（期望开启）"就会被显示成"开启"，看起来像下发过一次控制，误导人。
     */
    private static final String VERIFY_PASS_OPERATION = "复查通过";

    private final ILightingPlanService planService;
    private final ILightingPlanExecuteLogService executeLogService;
    private final ILightingCircuitService circuitService;
    private final ILightingSceneService sceneService;
    private final LightingSceneDetailMapper sceneDetailMapper;
    private final IBusinessConfigService businessConfigService;
    private final ILightingOperationLogService lightingOperationLogService;

    @Override
    public void registerVerify(Long planId, String version, String executeDate) {
        if (planId == null) {
            return;
        }
        // 全局开关：关闭（含未配置/填错/读取异常）时不登记，相当于整个功能未启用
        if (!isVerifyOn()) {
            return;
        }
        try {
            // 定位本次执行的日志行（最新一条）
            LightingPlanExecuteLog logRow = executeLogService.getOne(new LambdaQueryWrapper<LightingPlanExecuteLog>()
                    .eq(LightingPlanExecuteLog::getPlanId, planId)
                    .eq(StringUtils.isNotEmpty(version), LightingPlanExecuteLog::getVersion, version)
                    .eq(StringUtils.isNotEmpty(executeDate), LightingPlanExecuteLog::getExecuteDate, executeDate)
                    .orderByDesc(LightingPlanExecuteLog::getId)
                    .last("LIMIT 1"), false);
            if (logRow == null) {
                log.warn("【计划持续验证】未找到执行日志，跳过登记。planId={}, version={}, executeDate={}", planId, version, executeDate);
                return;
            }
            int delay = getDelayMinutes();
            LightingPlanExecuteLog upd = new LightingPlanExecuteLog();
            upd.setId(logRow.getId());
            upd.setVerifyStatus(LightingPlanExecuteLog.VERIFY_PENDING);
            upd.setVerifyTime(Date.from(LocalDateTime.now().plusMinutes(delay).atZone(ZoneId.systemDefault()).toInstant()));
            upd.setUpdateTime(new Date());
            executeLogService.updateById(upd);
            log.info("【计划持续验证】登记待验证成功：planId={}, executeDate={}, 延迟={}分钟", planId, executeDate, delay);
        } catch (Exception e) {
            log.error("【计划持续验证】登记失败 planId={}", planId, e);
        }
    }

    @Override
    public void executePendingVerify() {
        List<LightingPlanExecuteLog> dueList = executeLogService.list(new LambdaQueryWrapper<LightingPlanExecuteLog>()
                .eq(LightingPlanExecuteLog::getVerifyStatus, LightingPlanExecuteLog.VERIFY_PENDING)
                .isNotNull(LightingPlanExecuteLog::getVerifyTime)
                .le(LightingPlanExecuteLog::getVerifyTime, new Date())
                .orderByAsc(LightingPlanExecuteLog::getVerifyTime));
        if (dueList == null || dueList.isEmpty()) {
            return;
        }
        // 全局开关关闭：不复查，把到期的"待验证"直接置"已跳过"。
        // （必须处理：否则开关重新打开后，这些积压记录会拿"当时的灯状态"去比对旧执行结果，导致误补发）
        if (!isVerifyOn()) {
            for (LightingPlanExecuteLog row : dueList) {
                finish(row, LightingPlanExecuteLog.VERIFY_SKIPPED, "持续验证已关闭，跳过", row.getVerifyCount());
            }
            log.info("【计划持续验证】全局开关已关闭，跳过 {} 条到期验证记录", dueList.size());
            return;
        }
        for (LightingPlanExecuteLog row : dueList) {
            try {
                verifyOne(row);
            } catch (Exception e) {
                log.error("【计划持续验证】执行验证异常 logId={}, planId={}", row.getId(), row.getPlanId(), e);
                finish(row, LightingPlanExecuteLog.VERIFY_SKIPPED, "验证执行异常: " + e.getMessage(), row.getVerifyCount());
            }
        }
    }

    /**
     * 对单条到期记录执行**一轮**验证 + 按需补发；未到位且轮次未用尽时安排下一轮。
     * <p>
     * 轮次规则：总轮次取 business_config 的 {@code plan:verify:times}（缺省 1），每轮间隔取
     * {@code plan:verify:delay:minutes}；任一轮复查"全部到位"即提前结束（不再无谓下发）。
     * 每轮结果都在控制日志留痕（触发类型=持续验证）：有补发写父"持续验证补发 第N/M轮" + 回路子记录；
     * 无需补发写一条"持续验证复查通过 第N/M轮"（无子记录），保证"跑过但没事可补"也查得到。
     * verify_result 只留摘要（补了哪几个回路看控制日志子条目，避免 500 字符在多次验证时溢出）。
     */
    private void verifyOne(LightingPlanExecuteLog row) {
        int times = getVerifyTimes();
        int round = (row.getVerifyCount() == null ? 0 : row.getVerifyCount()) + 1;
        LightingPlan plan = planService.getById(row.getPlanId());
        if (plan == null) {
            finish(row, LightingPlanExecuteLog.VERIFY_SKIPPED, "计划不存在，跳过验证", row.getVerifyCount());
            return;
        }
        boolean expectOpen = LightingPlan.OPERATION_TYPE_OPEN.equals(plan.getOperationType());

        List<LightingCircuit> circuits = collectTargetCircuits(plan);
        if (circuits == null || circuits.isEmpty()) {
            finish(row, LightingPlanExecuteLog.VERIFY_DONE,
                    String.format("第%d/%d轮 无可用回路目标（可能仅含节目或目标已删除），未执行复查", round, times), round);
            return;
        }

        // 先挑出需要补发的回路：有补发写"补发"父日志 + 回路子记录；无补发写一条"复查通过"（无子记录）
        Map<Long, String> toFix = new LinkedHashMap<>();
        int checked = 0;
        for (LightingCircuit c : circuits) {
            if (c == null || c.getId() == null) {
                continue;
            }
            checked++;
            boolean isOn = LightingCircuit.STATUS_ON.equals(c.getStatus());
            boolean needFix = expectOpen ? !isOn : isOn;
            if (needFix) {
                toFix.put(c.getId(), circuitName(c));
            }
        }

        int fixedCount = 0;
        if (!toFix.isEmpty()) {
            // 父日志：本轮的补偿父记录（控制日志里显示"持续验证补发 第N/M轮"），子记录挂其下；
            // 写失败返回 null，此时回落为单参 open/close（日志问题不影响补发本身）
            Long parentLogId = saveVerifyParentLog(plan, expectOpen, round, times);
            for (Long circuitId : toFix.keySet()) {
                try {
                    controlCircuit(circuitId, expectOpen, parentLogId);
                    fixedCount++;
                } catch (Exception e) {
                    log.error("【计划持续验证】补下发失败 circuitId={}, 期望={}", circuitId, expectOpen ? "开启" : "关闭", e);
                }
            }
        } else {
            // 复查通过（本轮没有任何回路需要补发）：留一条"复查通过"日志，
            // 让"第N轮确实跑过、且无需补发"在控制日志里可见（否则只存在于 verify_result，控制日志查不到）
            saveVerifyPassLog(plan, round, times, checked, expectOpen);
        }

        String expectLabel = expectOpen ? "开启" : "关闭";
        // verify_result 只记摘要：补了哪几个回路看控制日志子条目，避免 500 字符在多次验证时溢出
        String roundResult = toFix.isEmpty()
                ? String.format("第%d/%d轮 复查 %d 个回路，状态均已%s，无需补发", round, times, checked, expectLabel)
                : String.format("第%d/%d轮 复查 %d 个回路，%d 个未%s，已补发 %d 个",
                        round, times, checked, toFix.size(), expectLabel, fixedCount);

        boolean allDone = toFix.isEmpty();
        if (allDone || round >= times) {
            String suffix = allDone && round < times ? "，已全部到位提前结束" : "";
            finish(row, LightingPlanExecuteLog.VERIFY_DONE, roundResult + suffix, round);
            log.info("【计划持续验证】planId={}, planName={} {} 轮完成（共 {} 轮上限）：{}",
                    plan.getId(), plan.getPlanName(), round, times, roundResult);
        } else {
            scheduleNextRound(row, roundResult, round);
            log.info("【计划持续验证】planId={}, planName={} 第{}/{}轮仍有未到位，{} 分钟后再验：{}",
                    plan.getId(), plan.getPlanName(), round, times, getDelayMinutes(), roundResult);
        }
    }

    /**
     * 安排下一轮验证：仍置"待验证"，verify_time = now + 间隔，并累计已执行轮次
     */
    private void scheduleNextRound(LightingPlanExecuteLog row, String lastRoundResult, int finishedRound) {
        try {
            int delay = getDelayMinutes();
            LightingPlanExecuteLog upd = new LightingPlanExecuteLog();
            upd.setId(row.getId());
            upd.setVerifyStatus(LightingPlanExecuteLog.VERIFY_PENDING);
            upd.setVerifyResult(truncate(lastRoundResult + "，将于 " + delay + " 分钟后再验", RESULT_MAX_LEN));
            upd.setVerifyCount(finishedRound);
            upd.setVerifyTime(Date.from(LocalDateTime.now().plusMinutes(delay).atZone(ZoneId.systemDefault()).toInstant()));
            upd.setUpdateTime(new Date());
            executeLogService.updateById(upd);
        } catch (Exception e) {
            log.error("【计划持续验证】安排下一轮失败 logId={}", row.getId(), e);
            finish(row, LightingPlanExecuteLog.VERIFY_SKIPPED, "安排下一轮验证失败: " + e.getMessage(), row.getVerifyCount());
        }
    }

    /**
     * 全局验证总轮次（含首次）：business_config 的 {@code plan:verify:times}。
     * 缺省/填错/小于 1 一律按 1 处理（1 = 只验一次，与多次验证改造前一致）。
     */
    private int getVerifyTimes() {
        try {
            Long v = businessConfigService.getLongByKey(BusinessConfigConstant.PLAN_VERIFY_TIMES);
            if (v != null && v >= 1) {
                return v.intValue();
            }
        } catch (Exception e) {
            log.warn("【计划持续验证】读取验证轮次配置失败，使用默认值 {} 轮", DEFAULT_VERIFY_TIMES, e);
        }
        return DEFAULT_VERIFY_TIMES;
    }

    /**
     * 写一条"持续验证补发"父日志（顶层），让补偿动作在控制日志里可辨认：
     * 名称形如 {@code 计划名（持续验证补发 第1/3轮）}、**触发类型="持续验证"**（控制日志可直接按该值筛选）。
     * 写日志失败不影响补发，返回 null 由调用方回落为不带父日志的控制。
     */
    private Long saveVerifyParentLog(LightingPlan plan, boolean expectOpen, int round, int times) {
        try {
            boolean isScenePlan = LightingPlan.REL_TYPE_SCENE.equals(plan.getRelType());
            LightingOperationLog logRow = new LightingOperationLog();
            logRow.setLogType(isScenePlan ? LightingOperationLog.LOG_TYPE_SCENE_PLAN : LightingOperationLog.LOG_TYPE_PLAN);
            logRow.setParentId(null);
            logRow.setRelType(isScenePlan ? LightingPlan.REL_TYPE_SCENE : "定时任务");
            logRow.setRelId(plan.getId());
            logRow.setName(plan.getPlanName() + "（" + VERIFY_LOG_TAG + " 第" + round + "/" + times + "轮）");
            logRow.setOperationTime(LocalDateTime.now());
            logRow.setOperationType(expectOpen ? "开启" : "关闭");
            // 操作人与计划定时执行保持一致；补偿性质由"触发类型=持续验证"体现
            logRow.setOperationBy("照明计划");
            // 子回路日志通过 resolveOperatorType(parentId) 继承该值，同样是"持续验证"
            logRow.setOperatorType(LightingOperationLog.OPERATOR_TYPE_VERIFY);
            lightingOperationLogService.save(logRow);
            return logRow.getId();
        } catch (Exception e) {
            log.error("【计划持续验证】写补偿父日志失败 planId={}，本次补发不带父日志", plan.getId(), e);
            return null;
        }
    }

    /**
     * 写一条"复查通过"日志（顶层，**无子记录**）：本轮复查全部到位、未做任何补发时留痕。
     * <p>
     * 目的：让"第N轮跑过且无需补发"这件事在控制日志里也能查到——此前只有补发才有日志，
     * 于是"第2轮明明执行了（verify_result 已写）却在控制日志里没有任何痕迹"，容易被误判成没执行。
     * <p>
     * 名称形如 {@code 计划名（持续验证复查通过 第2/2轮：复查 8 个回路均已开启）}，
     * **触发类型="持续验证"**（可筛选）；操作类型固定"复查通过"（见 {@link #VERIFY_PASS_OPERATION} 注释）。
     * 写日志失败只记错误日志，不影响验证结果与轮次推进。
     */
    private void saveVerifyPassLog(LightingPlan plan, int round, int times, int checked, boolean expectOpen) {
        try {
            boolean isScenePlan = LightingPlan.REL_TYPE_SCENE.equals(plan.getRelType());
            LightingOperationLog logRow = new LightingOperationLog();
            logRow.setLogType(isScenePlan ? LightingOperationLog.LOG_TYPE_SCENE_PLAN : LightingOperationLog.LOG_TYPE_PLAN);
            logRow.setParentId(null);
            logRow.setRelType(isScenePlan ? LightingPlan.REL_TYPE_SCENE : "定时任务");
            logRow.setRelId(plan.getId());
            logRow.setName(plan.getPlanName() + "（" + VERIFY_PASS_TAG + " 第" + round + "/" + times + "轮：复查 "
                    + checked + " 个回路均已" + (expectOpen ? "开启" : "关闭") + "）");
            logRow.setOperationTime(LocalDateTime.now());
            logRow.setOperationType(VERIFY_PASS_OPERATION);
            // 与补偿日志同口径：操作人固定"照明计划"，性质由"触发类型=持续验证"体现
            logRow.setOperationBy("照明计划");
            logRow.setOperatorType(LightingOperationLog.OPERATOR_TYPE_VERIFY);
            lightingOperationLogService.save(logRow);
        } catch (Exception e) {
            log.error("【计划持续验证】写复查通过日志失败 planId={}，不影响验证结果", plan.getId(), e);
        }
    }

    /**
     * 补发单个回路：有父日志时挂到父日志下（控制日志可追溯），父日志缺失时回落为直接控制
     */
    private void controlCircuit(Long circuitId, boolean expectOpen, Long parentLogId) {
        if (expectOpen) {
            if (parentLogId == null) {
                circuitService.open(circuitId);
            } else {
                circuitService.open(circuitId, parentLogId);
            }
        } else {
            if (parentLogId == null) {
                circuitService.close(circuitId);
            } else {
                circuitService.close(circuitId, parentLogId);
            }
        }
    }

    /**
     * 回路展示名：回路名称 → 回路编码 → ID
     */
    private String circuitName(LightingCircuit circuit) {
        return StringUtils.isNotEmpty(circuit.getCircuitName()) ? circuit.getCircuitName()
                : (StringUtils.isNotEmpty(circuit.getCircuitCode()) ? circuit.getCircuitCode() : String.valueOf(circuit.getId()));
    }

    /**
     * 把计划目标展开成回路列表：
     * 区域 → 区域下所有回路；回路 → 其本身；场景 → 场景明细展开（节目无回路状态，跳过）
     */
    private List<LightingCircuit> collectTargetCircuits(LightingPlan plan) {
        Set<Long> relIds = parseRelIds(plan.getRelIds());
        if (relIds.isEmpty()) {
            return new ArrayList<>();
        }
        String relType = plan.getRelType();
        if (LightingPlan.REL_TYPE_AREA.equals(relType)) {
            List<LightingCircuit> list = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                    .in(LightingCircuit::getAreaId, relIds));
            return list == null ? new ArrayList<>() : list;
        }
        if (LightingPlan.REL_TYPE_CIRCUIT.equals(relType)) {
            List<LightingCircuit> list = circuitService.listByIds(relIds);
            return list == null ? new ArrayList<>() : list;
        }
        if (LightingPlan.REL_TYPE_SCENE.equals(relType)) {
            return collectSceneCircuits(relIds);
        }
        return new ArrayList<>();
    }

    /**
     * 展开场景计划的目标回路：场景明细（区域/回路）→ 回路；仅含节目的场景无法用回路状态验证，跳过
     */
    private List<LightingCircuit> collectSceneCircuits(Set<Long> sceneIds) {
        List<LightingCircuit> result = new ArrayList<>();
        List<LightingScene> scenes = sceneService.listByIds(sceneIds);
        if (scenes == null || scenes.isEmpty()) {
            return result;
        }
        for (LightingScene scene : scenes) {
            if (scene == null) {
                continue;
            }
            List<LightingSceneDetail> details = sceneDetailMapper.selectList(
                    new LambdaQueryWrapper<LightingSceneDetail>()
                            .eq(LightingSceneDetail::getSceneId, scene.getId()));
            if (details == null || details.isEmpty()) {
                if (StringUtils.isNotEmpty(scene.getProgramSceneIds())) {
                    log.info("【计划持续验证】场景[{}]仅含节目，无回路状态可验证，跳过", scene.getSceneName());
                }
                continue;
            }
            Set<Long> areaIds = new HashSet<>();
            Set<Long> circuitIds = new HashSet<>();
            for (LightingSceneDetail d : details) {
                if (d.getRelId() == null) {
                    continue;
                }
                if (LightingScene.REL_TYPE_AREA.equals(d.getRelType())) {
                    areaIds.add(d.getRelId());
                } else if (LightingScene.REL_TYPE_CIRCUIT.equals(d.getRelType())) {
                    circuitIds.add(d.getRelId());
                }
            }
            if (!areaIds.isEmpty()) {
                List<LightingCircuit> byArea = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                        .in(LightingCircuit::getAreaId, areaIds));
                if (byArea != null) {
                    result.addAll(byArea);
                }
            }
            if (!circuitIds.isEmpty()) {
                List<LightingCircuit> byId = circuitService.listByIds(circuitIds);
                if (byId != null) {
                    result.addAll(byId);
                }
            }
        }
        return result;
    }

    private void finish(LightingPlanExecuteLog row, String verifyStatus, String verifyResult, Integer verifyCount) {
        try {
            LightingPlanExecuteLog upd = new LightingPlanExecuteLog();
            upd.setId(row.getId());
            upd.setVerifyStatus(verifyStatus);
            upd.setVerifyResult(truncate(verifyResult, RESULT_MAX_LEN));
            upd.setVerifyCount(verifyCount);
            upd.setUpdateTime(new Date());
            executeLogService.updateById(upd);
        } catch (Exception e) {
            log.error("【计划持续验证】更新验证结果失败 logId={}", row.getId(), e);
        }
    }

    /**
     * 全局持续验证开关（business_config: plan:verify:enabled）。
     * 只认"开启"两字：填"关闭"、留空、填错、读取异常一律视为关闭。
     */
    private boolean isVerifyOn() {
        try {
            String value = businessConfigService.getValueByKey(BusinessConfigConstant.PLAN_VERIFY_ENABLED);
            return SWITCH_ON.equals(StringUtils.trimToEmpty(value));
        } catch (Exception e) {
            log.warn("【计划持续验证】读取全局开关失败，按关闭处理", e);
            return false;
        }
    }

    private int getDelayMinutes() {
        try {
            Long v = businessConfigService.getLongByKey(BusinessConfigConstant.PLAN_VERIFY_DELAY_MINUTES);
            if (v != null && v >= 0) {
                return v.intValue();
            }
        } catch (Exception e) {
            log.warn("【计划持续验证】读取延迟配置失败，使用默认值 {} 分钟", DEFAULT_DELAY_MINUTES, e);
        }
        return DEFAULT_DELAY_MINUTES;
    }

    private Set<Long> parseRelIds(String relIds) {
        Set<Long> ids = new HashSet<>();
        if (StringUtils.isBlank(relIds)) {
            return ids;
        }
        for (String s : relIds.split(",")) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            try {
                ids.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略非法ID
            }
        }
        return ids;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
