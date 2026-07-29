package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 地块运行时长VO
 */
@Data
@ApiModel(value = "地块运行时长", description = "各地块运行时长统计")
public class AreaRunTimeVo {

    @ApiModelProperty(value = "地块ID")
    private Long areaId;

    @ApiModelProperty(value = "地块名称")
    private String areaName;

    @ApiModelProperty(value = "运行时长（小时）")
    private Double runTime;
}
