package org.jeecg.modules.bems.job;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 计费点数据定时计算任务
 * 
 * 这是原 MeteringPointDataJob 中 TODO 逻辑的实现。
 * 可通过动态定时任务 API 配置此任务，beanName="energyDataSyncJob", methodName="calculateMeteringPointData"
 */
@Slf4j
@Component("energyDataSyncJob")
@AllArgsConstructor
public class EnergyDataSyncJob {

    private final IMeteringPointDataService meteringPointDataService;

    /**
     * 计算计费点数据
     * cron 建议: "0 15 * * * ?" (每小时的第15分钟)
     */
    public void calculateMeteringPointData() {
        log.info("===== 计费点数据计算开始 =====");
        long startTime = System.currentTimeMillis();
        try {
            meteringPointDataService.calculateValue(LocalDateTime.now());
            long cost = System.currentTimeMillis() - startTime;
            log.info("===== 计费点数据计算完成，耗时: {}ms =====", cost);
        } catch (Exception e) {
            log.error("计费点数据计算异常", e);
        }
    }
}
