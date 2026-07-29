package org.jeecg.modules.bems.energyAnalysis.util.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 阶梯计价
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LadderPricing {

    /**
     * 阶梯-起始（不包含）
     */
    private BigDecimal stepMin;

    /**
     * 阶梯-结束（包含）
     */
    private BigDecimal stepMax;
    /**
     * 价格
     */
    private BigDecimal pricing;
}
