package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitAlarmLog;

import java.time.LocalDateTime;
import java.util.List;

public interface ILightingCircuitAlarmLogService extends IService<LightingCircuitAlarmLog> {

    /**
     * 记录一条报警流水（标记报警时调用，status=报警中）
     */
    void logAlarm(LightingCircuitAlarmLog log);

    /**
     * 恢复指定回路的最近一条"报警中"流水（置为已恢复并回填恢复时间）
     *
     * @return 是否成功回填
     */
    boolean recoverAlarm(Long circuitId, LocalDateTime recoverTime);

    /**
     * 分页查询历史报警流水（可按 回路/区域/规则/状态/时间范围 过滤）
     */
    Page<LightingCircuitAlarmLog> pageQuery(Page<LightingCircuitAlarmLog> page,
                                            String circuitName, Long areaId, String ruleCode,
                                            String status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定回路最近一条"报警中"流水（用于恢复回填）
     */
    List<LightingCircuitAlarmLog> listActiveByCircuit(Long circuitId);
}
