package org.jeecg.modules.bems.energyAnalysis.service;

import org.jeecg.modules.bems.energyAnalysis.vo.Chat;
import org.jeecg.modules.bems.energyAnalysis.vo.CostVo;
import org.jeecg.modules.bems.energyAnalysis.vo.Table;
import org.jeecg.modules.bems.energyAnalysis.vo.chat.PieChat;

import java.time.LocalDate;
import java.util.List;

/**
 * 成本分析
 */
public interface ICostAnalysisService {

    Table findDay(String energyFlowDiagramIds, LocalDate localDate);

    Table findMonth(String energyFlowDiagramIds,LocalDate localDate);

    Table findYear(String energyFlowDiagramIds,LocalDate localDate);


    /**
     * 获取当月总成本
     * @param date 月份
     * @param pointIds 计量点位集合
     * @return 成本
     */
    CostVo getTotalCost(LocalDate date, List<Long> pointIds);

    /**
     * 获取各专业成本饼状图
     * @param date 月份
     * @param pointIds 计量点位集合
     */
    PieChat findSpecialtyPieChat(LocalDate date, List<Long> pointIds);

    /**
     * 获取每日成本
     * @param date 月份
     * @param pointIds 计量点位集合
     * @return
     */
    Chat findDayCost(LocalDate date,List<Long> pointIds);


}
