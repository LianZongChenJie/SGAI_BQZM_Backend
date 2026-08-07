package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecuteLog;
import org.jeecg.modules.bems.lighting.mapper.LightingPlanExecuteLogMapper;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecuteLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class LightingPlanExecuteLogServiceImpl extends ServiceImpl<LightingPlanExecuteLogMapper, LightingPlanExecuteLog> implements ILightingPlanExecuteLogService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

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
            data.setSendTime(LocalDateTime.now());
            data.setCreateTime(LocalDateTime.now());
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
                LightingPlanExecuteLog data = pendingList.get(0);
                data.setStatus(targetStatus);
                data.setConsumeTime(LocalDateTime.now());
                data.setRemark(remark);
                data.setUpdateTime(LocalDateTime.now());
                super.updateById(data);
            } else {
                // 无匹配的发送记录（历史消息/记录缺失）：补插一条结果记录，保证日历状态可查
                LightingPlanExecuteLog data = new LightingPlanExecuteLog();
                data.setPlanId(planId);
                data.setVersion(version);
                data.setExecuteDate(executeDate);
                data.setStatus(targetStatus);
                data.setConsumeTime(LocalDateTime.now());
                data.setRemark(remark);
                data.setCreateTime(LocalDateTime.now());
                super.save(data);
            }
        } catch (Exception e) {
            log.error("更新照明计划执行日志失败, planId={}, version={}", planId, version, e);
        }
    }
}
