package org.jeecg.modules.bems.dataBoard.service;

import org.jeecg.modules.bems.dataBoard.vo.StatisticsVo;
import org.jeecg.modules.bems.energyAnalysis.vo.Chat;

import java.time.LocalDateTime;
import java.util.List;

public interface IDataBoardService {
    /**
     * 获取能耗统计信息
     * @param dateType 日期类型，day,month,year
     * @return 返回能耗统计信息的对象，如果没有统计信息则返回null
     */
    List<StatisticsVo> getEnergyConsumptionStatistics(String dateType);

    /**
     * 近七日电能耗趋势
     */
    Chat energyConsumptionPSDElectricity();

    /**
     * 近七日水能耗趋势
     */
    Chat energyConsumptionPSNWater();

    /**
     * 点位能耗趋势
     * @param pointId 点位id
     * @param name 名称
     * @param dateType 日期类型，day，month，year
     * @param time 时间
     * @return 能耗曲线
     */
    Chat energyConsumption(Long pointId, String name, String dateType, LocalDateTime time);
}
