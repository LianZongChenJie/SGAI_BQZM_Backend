package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecuteLog;
import org.jeecg.modules.bems.lighting.entity.LightingScene;
import org.jeecg.modules.bems.lighting.entity.LightingSceneDetail;
import org.jeecg.modules.bems.lighting.mapper.LightingSceneDetailMapper;
import org.jeecg.modules.bems.lighting.service.IBusinessConfigService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
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
import java.util.List;
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

    /** 业务配置 key：持续验证延迟分钟数（所有计划共用） */
    private static final String CONFIG_KEY_DELAY_MINUTES = "plan:verify:delay:minutes";
    /** 默认延迟分钟数（业务配置缺失时兜底） */
    private static final int DEFAULT_DELAY_MINUTES = 3;
    /** verify_result 列长度上限 */
    private static final int RESULT_MAX_LEN = 500;

    private final ILightingPlanService planService;
    private final ILightingPlanExecuteLogService executeLogService;
    private final ILightingCircuitService circuitService;
    private final ILightingSceneService sceneService;
    private final LightingSceneDetailMapper sceneDetailMapper;
    private final IBusinessConfigService businessConfigService;

    @Override
    public void registerVerify(Long planId, String version, String executeDate) {
        if (planId == null) {
            return;
        }
        try {
            LightingPlan plan = planService.getById(planId);
            if (plan == null || !isVerifyEnabled(plan)) {
                return;
            }
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
        for (LightingPlanExecuteLog row : dueList) {
            try {
                verifyOne(row);
            } catch (Exception e) {
                log.error("【计划持续验证】执行验证异常 logId={}, planId={}", row.getId(), row.getPlanId(), e);
                finish(row, LightingPlanExecuteLog.VERIFY_SKIPPED, "验证执行异常: " + e.getMessage());
            }
        }
    }

    /**
     * 对单条到期记录执行验证 + 按需补发
     */
    private void verifyOne(LightingPlanExecuteLog row) {
        LightingPlan plan = planService.getById(row.getPlanId());
        if (plan == null) {
            finish(row, LightingPlanExecuteLog.VERIFY_SKIPPED, "计划不存在，跳过验证");
            return;
        }
        boolean expectOpen = LightingPlan.OPERATION_TYPE_OPEN.equals(plan.getOperationType());

        List<LightingCircuit> circuits = collectTargetCircuits(plan);
        if (circuits == null || circuits.isEmpty()) {
            finish(row, LightingPlanExecuteLog.VERIFY_DONE, "无可用回路目标（可能仅含节目或目标已删除），未执行复查");
            return;
        }

        List<String> fixed = new ArrayList<>();
        int checked = 0;
        for (LightingCircuit c : circuits) {
            if (c == null || c.getId() == null) {
                continue;
            }
            checked++;
            boolean isOn = LightingCircuit.STATUS_ON.equals(c.getStatus());
            boolean needFix = expectOpen ? !isOn : isOn;
            if (!needFix) {
                continue;
            }
            String name = StringUtils.isNotEmpty(c.getCircuitName()) ? c.getCircuitName()
                    : (StringUtils.isNotEmpty(c.getCircuitCode()) ? c.getCircuitCode() : String.valueOf(c.getId()));
            try {
                if (expectOpen) {
                    circuitService.open(c.getId());
                } else {
                    circuitService.close(c.getId());
                }
                fixed.add(name);
            } catch (Exception e) {
                log.error("【计划持续验证】补下发失败 circuitId={}, 期望={}", c.getId(), expectOpen ? "开启" : "关闭", e);
            }
        }

        String result;
        String expectLabel = expectOpen ? "开启" : "关闭";
        if (fixed.isEmpty()) {
            result = String.format("复查 %d 个回路，状态均已%s，无需补发", checked, expectLabel);
        } else {
            result = String.format("复查 %d 个回路，%d 个未%s，已补下发：%s",
                    checked, fixed.size(), expectLabel, String.join("、", fixed));
        }
        finish(row, LightingPlanExecuteLog.VERIFY_DONE, result);
        log.info("【计划持续验证】planId={}, planName={} 复查完成：{}", plan.getId(), plan.getPlanName(), result);
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

    private void finish(LightingPlanExecuteLog row, String verifyStatus, String verifyResult) {
        try {
            LightingPlanExecuteLog upd = new LightingPlanExecuteLog();
            upd.setId(row.getId());
            upd.setVerifyStatus(verifyStatus);
            upd.setVerifyResult(truncate(verifyResult, RESULT_MAX_LEN));
            upd.setUpdateTime(new Date());
            executeLogService.updateById(upd);
        } catch (Exception e) {
            log.error("【计划持续验证】更新验证结果失败 logId={}", row.getId(), e);
        }
    }

    private boolean isVerifyEnabled(LightingPlan plan) {
        return plan.getVerifyAfterExecute() != null && plan.getVerifyAfterExecute() == 1;
    }

    private int getDelayMinutes() {
        try {
            Long v = businessConfigService.getLongByKey(CONFIG_KEY_DELAY_MINUTES);
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
