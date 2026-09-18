package org.jeecg.modules.bems.lighting.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 配置日志查询参数。
 * <p>
 * 与 {@code LightingOperationLogQueryDto}（控制日志）保持同一套写法：
 * GET 绑定、时间段用 {@code LocalDateTime + @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")}。
 */
@ApiModel(value = "配置日志查询参数")
@Data
public class LightingConfigLogQueryDto {

    /**
     * 操作类型：新增、修改、删除
     */
    @ApiModelProperty(value = "操作类型：新增、修改、删除（精确匹配）")
    private String operType;

    /**
     * 操作模块：场景配置、定时控制 等
     */
    @ApiModelProperty(value = "操作模块：场景配置、定时控制 等（精确匹配）")
    private String operModule;

    /**
     * 操作人账号（精确匹配，对应 oper_by）
     */
    @ApiModelProperty(value = "操作人账号（精确匹配）")
    private String operBy;

    /**
     * 操作人姓名（模糊匹配，对应 oper_name）
     */
    @ApiModelProperty(value = "操作人姓名（模糊匹配）")
    private String operName;

    /**
     * 操作对象类型（精确匹配）：场景、定时任务 等
     */
    @ApiModelProperty(value = "操作对象类型：场景、定时任务 等（精确匹配）")
    private String targetType;

    /**
     * 操作对象ID（精确匹配）：查某条计划/场景的变更史
     */
    @ApiModelProperty(value = "操作对象ID（精确匹配）")
    private Long targetId;

    /**
     * 操作对象名称（模糊匹配）
     */
    @ApiModelProperty(value = "操作对象名称（模糊匹配）")
    private String targetName;

    /**
     * 操作内容关键字（模糊匹配）
     */
    @ApiModelProperty(value = "操作内容关键字（模糊匹配）")
    private String operContent;

    /**
     * 操作时间-起始（含边界）
     */
    @ApiModelProperty(value = "操作时间-起始 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 操作时间-结束（含边界）
     */
    @ApiModelProperty(value = "操作时间-结束 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "页码")
    private Integer pageNo = 1;

    @ApiModelProperty(value = "每页条数")
    private Integer pageSize = 10;
}
