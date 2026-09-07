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
 * 照明回路当前命中规则明细（一行 = 回路 × 规则）
 */
@Data
@TableName("lighting_circuit_alarm")
@ApiModel(value = "照明回路当前命中规则", description = "照明回路当前命中规则明细")
public class LightingCircuitAlarm {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 回路ID
     */
    @ApiModelProperty(value = "回路ID")
    private Long circuitId;

    /**
     * 回路编码
     */
    @ApiModelProperty(value = "回路编码")
    private String circuitCode;

    /**
     * 回路名称
     */
    @ApiModelProperty(value = "回路名称")
    private String circuitName;

    /**
     * 区域ID(箱)
     */
    @ApiModelProperty(value = "区域ID(箱)")
    private Long areaId;

    /**
     * 区域名称
     */
    @ApiModelProperty(value = "区域名称")
    private String areaName;

    /**
     * 空间编码(如 903)
     */
    @ApiModelProperty(value = "空间编码(如 903)")
    private String space;

    /**
     * 命中规则编码 R1/R2/R3/R4
     */
    @ApiModelProperty(value = "命中规则编码 R1/R2/R3/R4")
    private String ruleCode;

    /**
     * 命中规则名称
     */
    @ApiModelProperty(value = "命中规则名称")
    private String ruleName;

    /**
     * 命中级别：alarm报警 / warn预警 / note提示
     */
    @ApiModelProperty(value = "命中级别：alarm报警/warn预警/note提示")
    private String ruleLevel;

    /**
     * 命中时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "命中时间")
    private LocalDateTime alarmTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
