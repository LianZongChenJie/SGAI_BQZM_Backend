package org.jeecg.modules.bems.energyAnalysis.service;

import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.vo.CarbonEmissionDataVo;
import org.jeecg.modules.bems.energyAnalysis.vo.CarbonEmissionOverviewVo;
import org.jeecg.modules.bems.energyAnalysis.vo.Chat;
import org.jeecg.modules.bems.energyAnalysis.vo.EnergyFlowDiagramVo;
import org.jeecg.modules.bems.energyAnalysis.vo.chat.PieChat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 碳排放分析
 */
public interface ICarbonEmissionService {

    List<EnergyFlowDiagramVo> getCarbonFlowChart(LocalDate date);

    CarbonEmissionOverviewVo getOverview(LocalDate date);

    Chat getTrendComparison(String type,LocalDate date,LocalDate compareDate);

    List<MeteringPoint> spatialList();

    List<MeteringPoint> specialtyList();

    /**
     * 碳对象碳排分析-饼图
     */
    PieChat getSpatialCarbonEmissionAnalysis(LocalDate date);

    /**
     * 碳对象碳排分析-柱状图
     * @param type 类型。日、月、年
     * @param date 日期
     * @param pointIds 点位id
     */
    Chat getSpatialCarbonEmission(String type, LocalDate date, List<Long> pointIds);

    /**
     * 场景化碳排分析-饼图
     */
    PieChat getSpecialtyCarbonEmissionAnalysis(LocalDate date);

    /**
     * 场景化碳排分析-柱状图
     * @param type 类型。日、月、年
     * @param date 日期
     * @param pointIds 点位id
     */
    Chat getSpecialtyCarbonEmission(String type, LocalDate date, List<Long> pointIds);

    /**
     * 获取碳排数据-日
     */
    CarbonEmissionDataVo getCarbonEmissionForDay(LocalDateTime dateTime);
    /**
     * 获取碳排数据-周
     */
    CarbonEmissionDataVo getCarbonEmissionForWeek(LocalDate date);
    /**
     * 获取碳排数据-月
     */
    CarbonEmissionDataVo getCarbonEmissionForMonth(LocalDate date);
    /**
     * 获取碳排数据-季
     */
    CarbonEmissionDataVo getCarbonEmissionForQuarter(LocalDate date);
    /**
     * 获取碳排数据-年
     */
    CarbonEmissionDataVo getCarbonEmissionForYear(LocalDate date);
}
