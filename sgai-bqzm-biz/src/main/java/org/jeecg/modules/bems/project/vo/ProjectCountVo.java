package org.jeecg.modules.bems.project.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectCountVo {
    //投资额(万元)
    private BigDecimal investmentAmountCount;
    //已完成数
    private Long completedCount;
    //总数
    private Long totalCount;

    /**
     * 收益
     */
    private String profit;
}
