package org.jeecg.modules.bems.lighting.service;

import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.vo.AreaStatisticsVo;
import org.jeecg.modules.bems.lighting.vo.EnergyStatisticsVo;
import org.jeecg.modules.bems.lighting.vo.OnlineStatisticsVo;

import java.util.List;

/**
 * 首页概览服务
 */
public interface ILightingHomeService {

    /**
     * 地块数量和覆盖度统计
     */
    AreaStatisticsVo getAreaStatistics();

    /**
     * 在线设备和在线率统计
     */
    OnlineStatisticsVo getOnlineStatistics();

    /**
     * 今日用电和较昨日对比
     */
    EnergyStatisticsVo getEnergyStatistics();

    /**
     * 地块运行状态列表
     */
    List<LightingArea> getAreaRunStatus(String space);

    /**
     * 一键全开
     */
    void openAll();

    /**
     * 一键全关
     */
    void closeAll();
}
