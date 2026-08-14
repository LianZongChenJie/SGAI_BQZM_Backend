package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 能耗排名/汇总项
 */
@Data
@ApiModel(value = "能耗排名项", description = "按地块/区域/箱子聚合的能耗指标")
public class EnergyRankItemVo {

    @ApiModelProperty(value = "名称（如：三高炉项目 / 本体）")
    private String name;

    @ApiModelProperty(value = "电表数")
    private Integer meters;

    @ApiModelProperty(value = "装机功率（kW）")
    private BigDecimal kw;

    @ApiModelProperty(value = "今日用电量（kWh）")
    private BigDecimal today;

    @ApiModelProperty(value = "本月用电量（kWh）")
    private BigDecimal month;

    @ApiModelProperty(value = "今日占比（百分比，如 17.2）")
    private BigDecimal ratio;
}
