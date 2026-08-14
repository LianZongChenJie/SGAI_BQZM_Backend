package org.jeecg.modules.bems.lighting.job;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.service.ILightingEnergyHourService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 照明能耗-小时整点统计任务
 * 每小时整点后 2 分钟聚合上一个整点的用电量（保证上一小时最后一分钟的读数已入库）
 */
@Component
@AllArgsConstructor
@Slf4j
public class LightingEnergyJob {

    private final ILightingEnergyHourService energyHourService;

    @Scheduled(cron = "0 2 * * * ?")
    public void aggregateLastHour() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime hourStart = now.withMinute(0).withSecond(0).withNano(0).minusHours(1);
        try {
            energyHourService.aggregateHour(hourStart);
        } catch (Exception e) {
            log.error("【能耗统计】小时电量统计异常：hour={}", hourStart, e);
        }
    }
}
