package org.jeecg.modules.bems.energyAnalysis.service;

import org.jeecg.modules.bems.energyAnalysis.vo.CostAccountingVo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ICostAccountingService {

    /**
     * 日成本计算
     * @param costCenterId 成本中心id
     * @param day 日期
     */
    List<CostAccountingVo> findCostByDay(Long costCenterId, LocalDate day);

    /**
     * 月成本计算
     * @param costCenterId 成本中心id
     * @param month 月份
     */
    List<CostAccountingVo> findCostByMonth(Long costCenterId,LocalDate month);

    /**
     * 年成本计算
     * @param costCenterId 成本中心id
     * @param year 年份
     */
    List<CostAccountingVo> findCostByYear(Long costCenterId,LocalDate year);

    /**
     * 计算用能成本
     * @param type 关联类型
     * @param relId 关联id
     * @param hour 小时
     * @param value 能耗
     */
    void calculationCost(String type, Long relId, LocalDateTime hour, BigDecimal value);
}
