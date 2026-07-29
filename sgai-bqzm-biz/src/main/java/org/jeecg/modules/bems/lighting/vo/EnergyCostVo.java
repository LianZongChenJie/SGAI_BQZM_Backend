package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用电成本分析VO
 */
@Data
@ApiModel(value = "用电成本分析", description = "用电成本分类统计")
public class EnergyCostVo {

    @ApiModelProperty(value = "成本类型")
    private String costType;

    @ApiModelProperty(value = "成本名称")
    private String costName;

    @ApiModelProperty(value = "金额（元）")
    private BigDecimal amount;

    @ApiModelProperty(value = "占比（百分比）")
    private BigDecimal percentage;
}
