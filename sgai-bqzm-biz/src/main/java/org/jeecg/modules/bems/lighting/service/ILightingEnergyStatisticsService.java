package org.jeecg.modules.bems.lighting.service;

import org.jeecg.modules.bems.lighting.vo.EnergyMeterReadVo;
import org.jeecg.modules.bems.lighting.vo.EnergyProportionVo;
import org.jeecg.modules.bems.lighting.vo.EnergyRankItemVo;
import org.jeecg.modules.bems.lighting.vo.EnergySummaryItemVo;
import org.jeecg.modules.bems.lighting.vo.EnergySummaryNodeVo;
import org.jeecg.modules.bems.lighting.vo.EnergyTrendVo;

import java.util.List;

/**
 * 能耗统计接口（对应原型"能耗统计"页）
 */
public interface ILightingEnergyStatisticsService {

    /**
     * 能耗排名（今日 kWh，降序 Top N）
     *
     * @param level 统计级别：parcel-按地块、zone-按区域、box-按箱子
     * @param date  日期（yyyy-MM-dd 或 yyyyMMdd），空默认今天
     * @param top   取前 N 名，空默认 15
     */
    List<EnergyRankItemVo> ranking(String level, String date, Integer top);

    /**
     * 占比（Top5 + 其他）
     */
    List<EnergyProportionVo> proportion(String level, String date);

    /**
     * 逐时趋势（按地块时为 Top5 对比，其他级别为全园单序列）
     */
    EnergyTrendVo hourlyTrend(String level, String date);

    /**
     * 汇总表（地块 → 区域 → 箱子 三层树）
     */
    List<EnergySummaryNodeVo> summary(String date);

    /**
     * 汇总表列表（仅网关维度，一行一个网关）
     * 由原树结构改为列表形式，支持按片区、箱子名称过滤
     *
     * @param date       日期（yyyy-MM-dd 或 yyyyMMdd），空默认今天
     * @param districtId 片区id（精确过滤，可选）
     * @param boxName    箱子名称（网关，模糊过滤，可选）
     */
    List<EnergySummaryItemVo> summaryList(String date, Long districtId, String boxName);

    /**
     * 电表读数区间查询（汇总表页签）
     * 按片区（区域）/箱子（网关）/时间区间，查询每个网关的开始表底、结束表底与累计用电量
     *
     * @param districtId 片区ID（可选）
     * @param gateway    网关编号（可选）
     * @param startTime  开始时间（yyyy-MM-dd HH:mm:ss，可选）
     * @param endTime    结束时间（yyyy-MM-dd HH:mm:ss，可选）
     */
    List<EnergyMeterReadVo> meterReads(Long districtId, String gateway, String startTime, String endTime);
}
