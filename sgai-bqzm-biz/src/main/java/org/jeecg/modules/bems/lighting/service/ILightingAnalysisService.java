package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.bems.lighting.vo.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 照明分析服务
 */
public interface ILightingAnalysisService {

    // ========== 运行时长 ==========

    /**
     * 各地块运行时长（柱状图）
     */
    List<AreaRunTimeVo> getAreaRunTime(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 运行时长对比
     */
    List<AreaRunTimeVo> getRunTimeCompare(List<Long> areaIds, LocalDateTime startTime, LocalDateTime endTime);

    // ========== 用电量 ==========

    /**
     * 各地块用电量（折线图）
     */
    List<AreaEnergyVo> getAreaEnergy(LocalDate startDate, LocalDate endDate);

    /**
     * 用电成本分析（环图）
     */
    List<EnergyCostVo> getEnergyCost(LocalDate startDate, LocalDate endDate);

    /**
     * 用电明细-查询列表
     */
    IPage<EnergyDetailVo> getEnergyDetail(int pageNo, int pageSize, Long areaId, LocalDate startDate, LocalDate endDate);

    // ========== 运行特性 ==========

    /**
     * 园区整体运行特性（折线图）
     */
    List<RunCharacteristicVo> getRunCharacteristic(LocalDate date);

    /**
     * 夜间运行模式分析（柱状图）
     */
    List<NightRunModeVo> getNightRunMode(LocalDate startDate, LocalDate endDate);
}
