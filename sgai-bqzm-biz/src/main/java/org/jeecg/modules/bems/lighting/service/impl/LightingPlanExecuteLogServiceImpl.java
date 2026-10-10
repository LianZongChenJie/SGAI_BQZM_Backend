package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecuteLog;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.mapper.LightingPlanExecuteLogMapper;
import org.jeecg.modules.bems.lighting.mapper.LightingPlanExecutionTimeMapper;
import org.jeecg.modules.bems.lighting.mapper.LightingPlanMapper;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecuteLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 照明计划执行日志（MQ 发送/消费追踪）
 * <p>
 * 注：判断"计划是否还在"用的是 mapper 直查，而不是注入 ILightingPlanService ——
 * 后者在删除计划时会调用本服务（清理/保留执行日志），反向注入服务会形成循环依赖。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LightingPlanExecuteLogServiceImpl extends ServiceImpl<LightingPlanExecuteLogMapper, LightingPlanExecuteLog> implements ILightingPlanExecuteLogService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 计划表（只读：判断计划是否还在、补全名称） */
    private final LightingPlanMapper lightingPlanMapper;

    /** 计划执行时间配置（只读：补全执行时间） */
    private final LightingPlanExecutionTimeMapper lightingPlanExecutionTimeMapper;

    @Override
    public void recordSend(Long planId, String planName, String version, LocalDateTime executionTime) {
        try {
            LightingPlanExecuteLog data = new LightingPlanExecuteLog();
            data.setPlanId(planId);
            data.setPlanName(planName);
            data.setVersion(version);
            data.setExecuteDate(executionTime != null ? executionTime.format(DATE_FORMATTER) : LocalDateTime.now().format(DATE_FORMATTER));
            data.setExecutionTime(executionTime != null ? executionTime.format(TIME_FORMATTER) : null);
            data.setStatus(LightingPlanExecuteLog.STATUS_PENDING);
            data.setSendTime(new Date());
            data.setCreateTime(new Date());
            super.save(data);
        } catch (Exception e) {
            // 记录失败不影响 MQ 发送主流程
            log.error("记录照明计划执行日志失败, planId={}", planId, e);
        }
    }

    @Override
    public void markConsumed(Long planId, String version, String executeDate, boolean success, String remark) {
        if (planId == null) {
            // 消息体解析失败拿不到 planId 时跳过，避免产生脏数据
            log.warn("markConsumed 跳过：planId 为空, version={}, executeDate={}", version, executeDate);
            return;
        }
        try {
            List<LightingPlanExecuteLog> pendingList = super.list(new LambdaQueryWrapper<LightingPlanExecuteLog>()
                    .eq(LightingPlanExecuteLog::getPlanId, planId)
                    .eq(LightingPlanExecuteLog::getVersion, version)
                    .eq(LightingPlanExecuteLog::getExecuteDate, executeDate)
                    .eq(LightingPlanExecuteLog::getStatus, LightingPlanExecuteLog.STATUS_PENDING)
                    .orderByDesc(LightingPlanExecuteLog::getId));

            String targetStatus = success ? LightingPlanExecuteLog.STATUS_SUCCESS : LightingPlanExecuteLog.STATUS_FAIL;

            if (pendingList != null && !pendingList.isEmpty()) {
                // 正常路径：回写发送时那条"待消费"记录（名称/执行时间/发送时间都保留）
                LightingPlanExecuteLog data = pendingList.get(0);
                data.setStatus(targetStatus);
                data.setConsumeTime(new Date());
                data.setRemark(remark);
                data.setUpdateTime(new Date());
                super.updateById(data);
                return;
            }

            // 走到这里说明没有匹配的"待消费"记录：计划已被删除（发送记录被一并清理）、历史消息、记录写库失败、或消息重复投递。
            // 原实现无条件补插一条结果记录，且不写 planName/executionTime/sendTime，日历上会被兜底渲染成
            // 「00:00:00 关灯【计划id】」这种无法辨认的脏数据。现规则：
            //   ① 计划已不存在 → 不补插（消息已无执行对象，记录它没有任何业务意义）；
            //   ② 同 (planId, version, executeDate) 已有结果记录 → 不补插（避免重复投递产生重复条目）；
            //   ③ 其余情况补插，但必须补全名称与执行时间。
            LightingPlan plan = lightingPlanMapper.selectById(planId);
            if (plan == null) {
                log.warn("markConsumed 跳过补插：计划已不存在，该条执行结果不再记录。planId={}, version={}, executeDate={}, success={}",
                        planId, version, executeDate, success);
                return;
            }
            if (existsResultLog(planId, version, executeDate)) {
                log.warn("markConsumed 跳过补插：已存在结果记录。planId={}, version={}, executeDate={}", planId, version, executeDate);
                return;
            }

            LightingPlanExecuteLog data = new LightingPlanExecuteLog();
            data.setPlanId(planId);
            data.setPlanName(plan.getPlanName());
            data.setVersion(version);
            data.setExecuteDate(executeDate);
            data.setExecutionTime(resolveExecutionTime(planId, version, plan));
            data.setStatus(targetStatus);
            data.setConsumeTime(new Date());
            data.setRemark(remark);
            data.setCreateTime(new Date());
            super.save(data);
            log.warn("markConsumed 补插执行结果记录（无发送记录）：planId={}, planName={}, executeDate={}, status={}",
                    planId, plan.getPlanName(), executeDate, targetStatus);
        } catch (Exception e) {
            log.error("更新照明计划执行日志失败, planId={}, version={}", planId, version, e);
        }
    }

    /** 是否已存在同 (planId, version, executeDate) 的结果记录（执行成功/执行失败） */
    private boolean existsResultLog(Long planId, String version, String executeDate) {
        LightingPlanExecuteLog exist = super.getOne(new LambdaQueryWrapper<LightingPlanExecuteLog>()
                .eq(LightingPlanExecuteLog::getPlanId, planId)
                .eq(LightingPlanExecuteLog::getVersion, version)
                .eq(LightingPlanExecuteLog::getExecuteDate, executeDate)
                .in(LightingPlanExecuteLog::getStatus,
                        LightingPlanExecuteLog.STATUS_SUCCESS, LightingPlanExecuteLog.STATUS_FAIL)
                .last("LIMIT 1"), false);
        return exist != null;
    }

    /** 补插记录时补全执行时间：优先执行时间配置（planId+version，退化为 planId），取不到再回落计划自身的 executionTime */
    private String resolveExecutionTime(Long planId, String version, LightingPlan plan) {
        String fromConfig = queryExecutionTime(planId, version);
        if (StringUtils.isBlank(fromConfig) && StringUtils.isNotBlank(version)) {
            fromConfig = queryExecutionTime(planId, null);
        }
        if (StringUtils.isNotBlank(fromConfig)) {
            return fromConfig;
        }
        return plan == null ? null : plan.getExecutionTime();
    }

    /** 按 planId（可选 version）取执行时间配置里的执行时刻 HH:mm:ss */
    private String queryExecutionTime(Long planId, String version) {
        try {
            LambdaQueryWrapper<LightingPlanExecutionTime> wrapper = new LambdaQueryWrapper<LightingPlanExecutionTime>()
                    .eq(LightingPlanExecutionTime::getPlanId, planId)
                    .orderByDesc(LightingPlanExecutionTime::getId);
            if (StringUtils.isNotBlank(version)) {
                wrapper.eq(LightingPlanExecutionTime::getVersion, version);
            }
            List<LightingPlanExecutionTime> list = lightingPlanExecutionTimeMapper.selectList(wrapper);
            if (list != null && !list.isEmpty()) {
                return list.get(0).getExecutionTime();
            }
        } catch (Exception e) {
            log.warn("补全执行时间失败（执行时间配置查询异常），planId={}", planId, e);
        }
        return null;
    }
}
