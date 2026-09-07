package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitAlarmLog;
import org.jeecg.modules.bems.lighting.mapper.LightingCircuitAlarmLogMapper;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitAlarmLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LightingCircuitAlarmLogServiceImpl
        extends ServiceImpl<LightingCircuitAlarmLogMapper, LightingCircuitAlarmLog>
        implements ILightingCircuitAlarmLogService {

    /** 状态：报警中 */
    public static final String STATUS_ACTIVE = "报警中";
    /** 状态：已恢复 */
    public static final String STATUS_RECOVERED = "已恢复";

    @Override
    public void logAlarm(LightingCircuitAlarmLog log) {
        if (log == null) {
            return;
        }
        if (log.getStatus() == null) {
            log.setStatus(STATUS_ACTIVE);
        }
        log.setId(null);
        super.save(log);
    }

    @Override
    public boolean recoverAlarm(Long circuitId, LocalDateTime recoverTime) {
        if (circuitId == null) {
            return false;
        }
        // 取该回路最近一条"报警中"流水
        List<LightingCircuitAlarmLog> active = list(new LambdaQueryWrapper<LightingCircuitAlarmLog>()
                .eq(LightingCircuitAlarmLog::getCircuitId, circuitId)
                .eq(LightingCircuitAlarmLog::getStatus, STATUS_ACTIVE)
                .orderByDesc(LightingCircuitAlarmLog::getId)
                .last("LIMIT 1"));
        if (active == null || active.isEmpty()) {
            return false;
        }
        LightingCircuitAlarmLog update = new LightingCircuitAlarmLog();
        update.setId(active.get(0).getId());
        update.setStatus(STATUS_RECOVERED);
        update.setRecoverTime(recoverTime != null ? recoverTime : LocalDateTime.now());
        return super.updateById(update);
    }

    @Override
    public Page<LightingCircuitAlarmLog> pageQuery(Page<LightingCircuitAlarmLog> page,
                                                   String circuitName, Long areaId, String ruleCode,
                                                   String status, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LightingCircuitAlarmLog> wrapper = new LambdaQueryWrapper<LightingCircuitAlarmLog>()
                .like(StringUtils.isNotBlank(circuitName), LightingCircuitAlarmLog::getCircuitName, circuitName)
                .eq(areaId != null, LightingCircuitAlarmLog::getAreaId, areaId)
                .eq(StringUtils.isNotBlank(ruleCode), LightingCircuitAlarmLog::getRuleCode, ruleCode)
                .eq(StringUtils.isNotBlank(status), LightingCircuitAlarmLog::getStatus, status)
                .ge(startTime != null, LightingCircuitAlarmLog::getAlarmTime, startTime)
                .le(endTime != null, LightingCircuitAlarmLog::getAlarmTime, endTime)
                .orderByDesc(LightingCircuitAlarmLog::getAlarmTime)
                .orderByDesc(LightingCircuitAlarmLog::getId);
        return super.page(page, wrapper);
    }

    @Override
    public List<LightingCircuitAlarmLog> listActiveByCircuit(Long circuitId) {
        return list(new LambdaQueryWrapper<LightingCircuitAlarmLog>()
                .eq(LightingCircuitAlarmLog::getCircuitId, circuitId)
                .eq(LightingCircuitAlarmLog::getStatus, STATUS_ACTIVE)
                .orderByDesc(LightingCircuitAlarmLog::getId)
                .last("LIMIT 1"));
    }
}
