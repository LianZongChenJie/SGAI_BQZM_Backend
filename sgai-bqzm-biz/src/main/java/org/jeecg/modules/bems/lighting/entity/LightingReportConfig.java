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
 * 报表配置表
 */
@Data
@TableName("lighting_report_config")
@ApiModel(value = "报表配置对象", description = "照明报表配置")
public class LightingReportConfig {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 报表名称
     */
    @ApiModelProperty(value = "报表名称")
    private String reportName;

    /**
     * 报表编码
     */
    @ApiModelProperty(value = "报表编码")
    private String reportCode;

    /**
     * 报表类型：运行报表、能耗报表、故障报表、告警报表、工单报表、自定义报表
     */
    @ApiModelProperty(value = "报表类型：运行报表、能耗报表、故障报表、告警报表、工单报表、自定义报表")
    private String reportType;

    /**
     * 报表描述
     */
    @ApiModelProperty(value = "报表描述")
    private String reportDesc;

    /**
     * 配置内容（JSON格式）
     */
    @ApiModelProperty(value = "配置内容（JSON格式）")
    private String configContent;

    /**
     * 状态：启用、禁用
     */
    @ApiModelProperty(value = "状态：启用、禁用")
    private String status;

    /**
     * 排序
     */
    @ApiModelProperty(value = "排序")
    private Integer sort;

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
