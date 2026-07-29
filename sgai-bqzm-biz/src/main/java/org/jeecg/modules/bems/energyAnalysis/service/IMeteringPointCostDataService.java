package org.jeecg.modules.bems.energyAnalysis.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IMeteringPointCostDataService {
    /**
     * 成本计算
     * @param pointId 计量点位id
     * @param hour 小时
     * @param value 能耗值
     */
    void calculationCost(Long pointId, LocalDateTime hour, BigDecimal value);
}
