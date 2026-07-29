package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用电明细VO
 */
@Data
@ApiModel(value = "用电明细", description = "用电明细记录")
public class EnergyDetailVo {

    @ApiModelProperty(value = "ID")
    private Long id;

    @ApiModelProperty(value = "地块名称")
    private String areaName;

    @ApiModelProperty(value = "回路名称")
    private String circuitName;

    @ApiModelProperty(value = "日期")
    private String date;

    @ApiModelProperty(value = "用电量（kWh）")
    private BigDecimal energy;

    @ApiModelProperty(value = "单价（元/kWh）")
    private BigDecimal price;

    @ApiModelProperty(value = "费用（元）")
    private BigDecimal cost;

    @ApiModelProperty(value = "统计时间")
    private LocalDateTime statTime;
}
