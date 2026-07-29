package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 成本核算
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CostAccountingVo {

    /**
     * 关联科目id
     */
    private Long id;

    /**
     * 关联类型
     */
    private String relType;

    /**
     * 关联id
     */
    private Long relId;

    /**
     * 成本科目名称
     */
    private String costAccountName;

    /**
     * 核算量
     */
    private BigDecimal accountingQuantity;

    /**
     * 核算成本
     */
    private BigDecimal accountingCost;
}
