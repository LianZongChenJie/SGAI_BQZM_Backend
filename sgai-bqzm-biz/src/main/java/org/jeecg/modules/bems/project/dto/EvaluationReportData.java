package org.jeecg.modules.bems.project.dto;

import lombok.Data;
import org.jeecg.modules.bems.energyAnalysis.vo.Chat;

import java.util.List;

@Data
public class EvaluationReportData {

    /**
     * 能耗单位
     */
    private String energyUnit;

    /**
     * 累计节约能耗
     */
    private String energySavings;
    /**
     * 累计节约成本
     */
    private String saveCosts;

    /**
     * 累计减碳（电）
     */
    private String carbonReductionAmount;

    /**
     * 能耗曲线图
     */
    private Chat energyCurve;

    /**
     * 能耗对标数据
     */
    private List<EnergyMarketData> energyMarket;
}
