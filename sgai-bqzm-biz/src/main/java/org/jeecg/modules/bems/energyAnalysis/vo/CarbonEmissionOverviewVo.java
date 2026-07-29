package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 碳排放总览
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CarbonEmissionOverviewVo {

    /**
     * 当月用能
     */
    private BigDecimal monthConsumption;
    /**
     * 较上月用能
     */
    private String monthConsumptionCompare;
    /**
     * 本季度用能
     */
    private BigDecimal quarterConsumption;
    /**
     * 较上季度用能
     */
    private String quarterConsumptionCompare;
    /**
     * 本年用能
     */
    private BigDecimal yearConsumption;
    /**
     * 较上年用能
     */
    private String yearConsumptionCompare;
    /**
     * 本月碳排
     */
    private BigDecimal monthCarbonEmission;
    /**
     * 较上月碳排
     */
    private String monthCarbonEmissionCompare;
    /**
     * 本季度碳排
     */
    private BigDecimal quarterCarbonEmission;
    /**
     * 较上季度碳排
     */
    private String quarterCarbonEmissionCompare;
    /**
     * 本年碳排
     */
    private BigDecimal yearCarbonEmission;
    /**
     * 较上年碳排
     */
    private String yearCarbonEmissionCompare;

}
