package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 能耗占比项（Top5 + 其他）
 */
@Data
@ApiModel(value = "能耗占比项", description = "占比图（Top5 + 其他）数据")
public class EnergyProportionVo {

    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "今日用电量（kWh）")
    private BigDecimal value;

    @ApiModelProperty(value = "今日占比（百分比）")
    private BigDecimal ratio;
}
