package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据类型分布VO
 */
@Data
@ApiModel(value = "数据类型分布", description = "数据类型分布")
public class DataTypeDistributionVo {

    @ApiModelProperty(value = "数据类型名称")
    private String typeName;

    @ApiModelProperty(value = "数据类型描述")
    private String typeDesc;

    @ApiModelProperty(value = "数据量")
    private Long count;

    @ApiModelProperty(value = "占比（百分比）")
    private BigDecimal percentage;
}
