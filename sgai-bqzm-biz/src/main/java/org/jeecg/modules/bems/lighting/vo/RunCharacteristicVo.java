package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运行特性VO
 */
@Data
@ApiModel(value = "运行特性", description = "园区整体运行特性")
public class RunCharacteristicVo {

    @ApiModelProperty(value = "时刻（如 00:00）")
    private String time;

    @ApiModelProperty(value = "总功率（kW）")
    private BigDecimal power;

    @ApiModelProperty(value = "在线率（百分比）")
    private BigDecimal onlineRate;
}
