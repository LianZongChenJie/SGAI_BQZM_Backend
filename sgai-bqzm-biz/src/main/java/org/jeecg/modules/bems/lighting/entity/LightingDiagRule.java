package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 照明回路智能诊断规则
 */
@Data
@TableName("lighting_diag_rule")
@ApiModel(value = "照明诊断规则对象", description = "照明回路智能诊断规则")
public class LightingDiagRule {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 规则编码：R1/R2/R3/R4/R5C/R5M
     */
    @ApiModelProperty(value = "规则编码：R1/R2/R3/R4/R5C/R5M")
    private String ruleCode;

    /**
     * 类型分类：数据/供电/线路/负载/控制
     */
    @ApiModelProperty(value = "类型分类：数据/供电/线路/负载/控制")
    private String ruleDomain;

    /**
     * 规则名称
     */
    @ApiModelProperty(value = "规则名称")
    private String name;

    /**
     * 严重程度：严重/预警/一般
     */
    @ApiModelProperty(value = "严重程度：严重/预警/一般")
    private String severity;

    /**
     * 一句话摘要
     */
    @ApiModelProperty(value = "一句话摘要")
    private String summary;

    /**
     * 规则机理/定义说明
     */
    @ApiModelProperty(value = "规则机理/定义说明")
    private String definition;

    /**
     * 命中级别：alarm报警 / warn预警 / note提示
     */
    @ApiModelProperty(value = "命中级别：alarm报警/warn预警/note提示")
    private String ruleLevel;

    /**
     * 启用/停用
     */
    @ApiModelProperty(value = "启用/停用")
    private String status;

    /**
     * 排序
     */
    @ApiModelProperty(value = "排序")
    private Integer sort;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
