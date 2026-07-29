package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 夜间运行模式分析VO
 */
@Data
@ApiModel(value = "夜间运行模式", description = "夜间运行模式分析")
public class NightRunModeVo {

    @ApiModelProperty(value = "星期（周一、周二...）")
    private String weekDay;

    @ApiModelProperty(value = "夜间运行时长（小时）")
    private Double nightRunTime;
}
