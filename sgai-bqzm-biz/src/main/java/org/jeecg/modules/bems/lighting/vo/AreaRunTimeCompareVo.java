package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 地块运行时长对比 VO（按地块 spaceName 分组）
 */
@Data
@ApiModel(value = "地块运行时长对比", description = "各地块运行时长对比统计")
public class AreaRunTimeCompareVo {

    @ApiModelProperty(value = "地块编码")
    private String space;

    @ApiModelProperty(value = "地块名称")
    private String spaceName;

    @ApiModelProperty(value = "回路数")
    private Long circuitCount;

    @ApiModelProperty(value = "总运行时长（小时）")
    private Double totalRunTime;

    @ApiModelProperty(value = "平均时长（h/日，每回路日均）")
    private Double avgRunTime;

    @ApiModelProperty(value = "同比（%，与上月同时段对比；暂无历史数据时为 null）")
    private Double yoy;
}
