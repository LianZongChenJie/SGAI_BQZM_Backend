package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 工单表
 */
@Data
@TableName("lighting_work_order")
@ApiModel(value = "工单对象", description = "照明工单管理")
public class LightingWorkOrder {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 工单编号
     */
    @ApiModelProperty(value = "工单编号")
    private String workOrderNo;

    /**
     * 工单标题
     */
    @ApiModelProperty(value = "工单标题")
    private String title;

    /**
     * 工单来源：报警工单、计划任务、手动创建
     */
    @ApiModelProperty(value = "工单来源：报警工单、计划任务、手动创建")
    private String source;

    /**
     * 关联类型：设备、回路、区域
     */
    @ApiModelProperty(value = "关联类型：设备、回路、区域")
    private String relatedType;

    /**
     * 关联ID
     */
    @ApiModelProperty(value = "关联ID")
    private Long relatedId;

    /**
     * 关联名称
     */
    @ApiModelProperty(value = "关联名称")
    private String relatedName;

    /**
     * 优先级：紧急、重要、一般
     */
    @ApiModelProperty(value = "优先级：紧急、重要、一般")
    private String priority;

    /**
     * 状态：待处理、处理中、已完成
     */
    @ApiModelProperty(value = "状态：待处理、处理中、已完成")
    private String status;

    /**
     * 工单描述
     */
    @ApiModelProperty(value = "工单描述")
    private String description;

    /**
     * 负责人ID
     */
    @ApiModelProperty(value = "负责人ID")
    private Long assignee;

    /**
     * 负责人姓名
     */
    @ApiModelProperty(value = "负责人姓名")
    private String assigneeName;

    /**
     * 处理时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "处理时间")
    private LocalDateTime handleTime;

    /**
     * 处理结果
     */
    @ApiModelProperty(value = "处理结果")
    private String handleResult;

    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    @ApiModelProperty(value = "更新人")
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 所属部门
     */
    @ApiModelProperty(value = "所属部门")
    private String sysOrgCode;
}
