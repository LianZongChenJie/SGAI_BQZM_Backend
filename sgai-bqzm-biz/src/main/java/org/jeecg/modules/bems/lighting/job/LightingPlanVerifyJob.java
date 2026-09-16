package org.jeecg.modules.bems.lighting.job;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.service.ILightingPlanVerifyService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 计划"持续验证"定时任务
 * <p>
 * 每分钟扫描到期的待验证记录（lighting_plan_execute_log.verify_status='待验证' 且 verify_time<=now），
 * 复查计划目标回路状态，未达成的重新下发开关指令，并更新验证结果。
 */
@Component
@AllArgsConstructor
@Slf4j
public class LightingPlanVerifyJob {

    private final ILightingPlanVerifyService planVerifyService;

    @Scheduled(cron = "0 * * * * ?")
    public void run() {
        try {
            planVerifyService.executePendingVerify();
        } catch (Exception e) {
            log.error("【计划持续验证】定时任务异常", e);
        }
    }
}
