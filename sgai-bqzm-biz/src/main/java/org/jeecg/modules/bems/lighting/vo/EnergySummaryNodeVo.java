package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 汇总表树节点（地块 → 区域 → 箱子）
 */
@Data
@ApiModel(value = "能耗汇总表节点", description = "地块/区域/箱子三层汇总")
public class EnergySummaryNodeVo {

    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "电表数")
    private Integer meters;

    @ApiModelProperty(value = "装机功率（kW）")
    private BigDecimal kw;

    @ApiModelProperty(value = "今日用电量（kWh）")
    private BigDecimal today;

    @ApiModelProperty(value = "本月用电量（kWh）")
    private BigDecimal month;

    @ApiModelProperty(value = "今日占比（百分比）")
    private BigDecimal ratio;

    @ApiModelProperty(value = "子节点（地块→区域→箱子）")
    private List<EnergySummaryNodeVo> children;
}
