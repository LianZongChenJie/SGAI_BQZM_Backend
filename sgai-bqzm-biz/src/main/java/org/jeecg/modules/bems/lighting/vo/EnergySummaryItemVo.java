package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 能耗汇总表-列表项 VO（按网关维度一行一条）
 * 由原树结构（地块→区域→箱子）改为列表形式，仅展示网关数据
 */
@Data
@ApiModel(value = "能耗汇总表列表项", description = "按网关维度展示的汇总数据")
public class EnergySummaryItemVo {

    @ApiModelProperty(value = "区域名称")
    private String areaName;

    @ApiModelProperty(value = "片区名称")
    private String districtName;

    @ApiModelProperty(value = "箱子名称（如：12号网关）")
    private String boxName;

    @ApiModelProperty(value = "网关编号")
    private String gatewayCode;

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

}
