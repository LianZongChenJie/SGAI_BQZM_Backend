package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 照明回路报警历史流水（一次完整报警生命周期）
 */
@Data
@TableName("lighting_circuit_alarm_log")
@ApiModel(value = "照明回路报警历史流水", description = "照明回路报警历史流水")
public class LightingCircuitAlarmLog {

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
     * 区域ID
     */
    @ApiModelProperty(value = "区域ID")
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
     * 命中规则编码 R1/R2
     */
    @ApiModelProperty(value = "命中规则编码 R1/R2")
    private String ruleCode;

    /**
     * 命中规则名称
     */
    @ApiModelProperty(value = "命中规则名称")
    private String ruleName;

    /**
     * 状态：报警中/已恢复
     */
    @ApiModelProperty(value = "状态：报警中/已恢复")
    private String status;

    /**
     * 报警开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "报警开始时间")
    private LocalDateTime alarmTime;

    /**
     * 恢复时间(已恢复时有)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "恢复时间")
    private LocalDateTime recoverTime;

    /**
     * 报警时实时电流(A)
     */
    @ApiModelProperty(value = "报警时实时电流(A)")
    private BigDecimal electricCurrent;

    /**
     * 报警时额定电流(A)
     */
    @ApiModelProperty(value = "报警时额定电流(A)")
    private BigDecimal ratedElectricCurrent;

    /**
     * 报警详情/判定依据
     */
    @ApiModelProperty(value = "报警详情/判定依据")
    private String detail;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
