package org.jeecg.modules.bems.lighting.job;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecutionTimeService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 照明计划定时任务
 */
@Component
@AllArgsConstructor
@Slf4j
public class LightingPlanJob {

    private final ILightingPlanService planService;

    private final LightingSendService sendService;

    private final ILightingPlanExecutionTimeService planExecutionTimeService;

    /**
     * 计算明天plan执行时间
     */
    @Scheduled(cron = "0 0 23 * * ?")
    public void calculationPlanExecutionTime(){
        List<LightingPlan> plans = planService.list();
        Map<Long,LightingPlanExecutionTime> planExecutionTimeMap = planExecutionTimeService.list()
                .stream()
                .collect(Collectors.toMap(LightingPlanExecutionTime::getPlanId, item -> item));
        if(CollectionUtil.isEmpty(plans) || MapUtil.isEmpty(planExecutionTimeMap)){
            return;
        }
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        for(LightingPlan plan : plans){
            try {
                // 定时任务同步的计划由动态调度器执行，老引擎不重复发 MQ
                if(plan.getScheduleJobId() != null){
                    continue;
                }
                LightingPlanExecutionTime data = planExecutionTimeMap.get(plan.getId());
                if(!LightingPlan.STATUS_ENABLE.equals(plan.getStatus()) || data == null){
                    continue;
                }
                // 获取执行时间配置
                // 判断日期是否在里面
                LocalDate startDate = data.getStartLocalDate();
                LocalDate endDate = data.getEndLocalDate();
                if(startDate != null && startDate.isAfter(tomorrow)){
                    continue;
                }
                if(endDate != null && endDate.isBefore(tomorrow)){
                    continue;
                }
                int week = tomorrow.getDayOfWeek().getValue();
                if(!data.getEnabledWeek().contains(String.valueOf(week))){
                    continue;
                }

                sendService.sendPlan(plan.getId(),plan.getPlanName(),data.getVersion(),tomorrow.atTime(data.getExecutionLocalTime()));
            }catch (Exception e){
                log.error("计划执行时间格式错误。计划id:{}", plan.getId());
            }
        }
    }

}
