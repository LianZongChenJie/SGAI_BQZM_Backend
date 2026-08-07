package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecuteLog;

import java.time.LocalDateTime;

public interface ILightingPlanExecuteLogService extends IService<LightingPlanExecuteLog> {

    /**
     * 记录计划 MQ 消息发送（新增一条 待消费 记录）
     *
     * @param planId        计划ID
     * @param planName      计划名称
     * @param version       计划版本号
     * @param executionTime 计划执行时间（用于推导执行日期 yyyy-MM-dd 和执行时间 HH:mm:ss）
     */
    void recordSend(Long planId, String planName, String version, LocalDateTime executionTime);

    /**
     * 记录 MQ 消息消费结果：
     * 优先将 (planId, version, executeDate) 下最新一条 待消费 记录更新为结果；
     * 若无匹配记录（如历史消息），补插一条结果记录，保证日历状态可查。
     *
     * @param planId      计划ID
     * @param version     计划版本号
     * @param executeDate 执行日期 yyyy-MM-dd
     * @param success     是否执行成功
     * @param remark      备注（失败原因等）
     */
    void markConsumed(Long planId, String version, String executeDate, boolean success, String remark);
}
