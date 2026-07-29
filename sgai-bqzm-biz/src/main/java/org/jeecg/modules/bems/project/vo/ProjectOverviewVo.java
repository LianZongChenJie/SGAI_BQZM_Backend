package org.jeecg.modules.bems.project.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 项目总览
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectOverviewVo {

    /**
     * 投资金额
     */
    private BigDecimal investmentAmount;

    /**
     * 节能收益
     */
    private BigDecimal energySavingBenefits;

    /**
     * 投资收益比
     */
    private String investmentReturnRatio;


}
