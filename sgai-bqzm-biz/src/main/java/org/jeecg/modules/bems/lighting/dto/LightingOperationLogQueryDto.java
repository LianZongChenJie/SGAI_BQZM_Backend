package org.jeecg.modules.bems.lighting.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@ApiModel(value = "控制日志查询参数")
@Data
public class LightingOperationLogQueryDto {

    /**
     * 关联类型：区域、回路
     */
    @ApiModelProperty(value = "关联类型：区域、回路")
    private String relType;

    /**
     * 操作类型：开、关（模糊匹配，兼容 区域全开/回路开启/区域全关/回路关闭 等写法）
     */
    @ApiModelProperty(value = "操作类型：开、关（模糊匹配）")
    private String operationType;

    /**
     * 操作时间-起始
     */
    @ApiModelProperty(value = "操作时间-起始 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 操作时间-结束
     */
    @ApiModelProperty(value = "操作时间-结束 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "页码")
    private Integer pageNo = 1;

    @ApiModelProperty(value = "每页条数")
    private Integer pageSize = 10;

}
