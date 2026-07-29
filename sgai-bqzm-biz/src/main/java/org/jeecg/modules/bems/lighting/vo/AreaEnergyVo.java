package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 地块用电量VO
 */
@Data
@ApiModel(value = "地块用电量", description = "各地块用电量统计")
public class AreaEnergyVo {

    @ApiModelProperty(value = "日期")
    private String date;

    @ApiModelProperty(value = "地块ID")
    private Long areaId;

    @ApiModelProperty(value = "地块名称")
    private String areaName;

    @ApiModelProperty(value = "用电量（kWh）")
    private BigDecimal energy;
}
