package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 碳排放 数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CarbonEmissionDataVo {

    /**
     * 排放量
     */
    private BigDecimal value;

    /**
     * 涨幅
     */
    private String increase;

    /**
     * 预测值
     */
    private BigDecimal prediction;

}
