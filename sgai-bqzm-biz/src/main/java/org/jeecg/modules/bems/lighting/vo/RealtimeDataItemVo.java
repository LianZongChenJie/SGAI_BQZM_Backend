package org.jeecg.modules.bems.lighting.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 实时数据项VO
 */
@Data
@ApiModel(value = "实时数据项", description = "实时数据流-数据项")
public class RealtimeDataItemVo {

    @ApiModelProperty(value = "时间戳")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @ApiModelProperty(value = "场所")
    private String spaceName;

    @ApiModelProperty(value = "设备")
    private String deviceName;

    @ApiModelProperty(value = "数据项")
    private String dataItem;

    @ApiModelProperty(value = "值")
    private String value;

    @ApiModelProperty(value = "类型：正常、异常、告警")
    private String type;

    @ApiModelProperty(value = "颜色（用于前端展示）")
    private String color;
}
