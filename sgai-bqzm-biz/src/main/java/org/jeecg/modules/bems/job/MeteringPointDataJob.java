package org.jeecg.modules.bems.job;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 计费点数据定时计算任务
 *
 * TODO（设计说明）: 未来应该由设备计量数据更新来触发计算（事件驱动），
 * 而不是靠定时轮询。当前保留定时轮询方式作为兜底。
 *
 * 此任务已改为动态调度方式。
 * 原 @Scheduled 注解已移除，改为通过 DynamicScheduleManager 管理。
 * 通过前端 API 添加定时任务时：
 *   beanName = "meteringPointDataJob"
 *   methodName = "calculationMeteringPointData"
 *   cronExpression = "0 15 * * * ?" (每小时第15分钟执行)
 */
@Component
@AllArgsConstructor
@Slf4j
public class MeteringPointDataJob {

    private final IMeteringPointDataService service;

    public void calculationMeteringPointData(){
        log.info("===== MeteringPointDataJob 开始执行 =====");
        long startTime = System.currentTimeMillis();
        try {
            service.calculateValue(LocalDateTime.now());
            long cost = System.currentTimeMillis() - startTime;
            log.info("===== MeteringPointDataJob 执行完成，耗时: {}ms =====", cost);
        } catch (Exception e) {
            log.error("MeteringPointDataJob 执行异常", e);
        }
    }
}
