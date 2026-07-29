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
 * 控制日志表
 */
@Data
@TableName("lighting_control_log")
@ApiModel(value = "控制日志对象", description = "照明控制日志")
public class LightingControlLog {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 控制时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "控制时间")
    private LocalDateTime controlTime;

    /**
     * 控制类型：场景、回路
     */
    @ApiModelProperty(value = "控制类型：场景、回路")
    private String controlType;

    /**
     * 关联类型：区域、回路、场景
     */
    @ApiModelProperty(value = "关联类型：区域、回路、场景")
    private String relType;

    /**
     * 关联ID
     */
    @ApiModelProperty(value = "关联ID")
    private Long relId;

    /**
     * 关联名称
     */
    @ApiModelProperty(value = "关联名称")
    private String relName;

    /**
     * 操作：自动执行、手动关闭、手动开启
     */
    @ApiModelProperty(value = "操作：自动执行、手动关闭、手动开启")
    private String operation;

    /**
     * 开灯时间
     */
    @ApiModelProperty(value = "开灯时间")
    private String openTime;

    /**
     * 关灯时间
     */
    @ApiModelProperty(value = "关灯时间")
    private String closeTime;

    /**
     * 操作类型：自动、手动
     */
    @ApiModelProperty(value = "操作类型：自动、手动")
    private String operatorType;

    /**
     * 操作人账号
     */
    @ApiModelProperty(value = "操作人账号")
    private String operatorBy;

    /**
     * 操作人姓名
     */
    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    /**
     * IP地址
     */
    @ApiModelProperty(value = "IP地址")
    private String ipAddress;

    /**
     * 执行结果：成功、失败
     */
    @ApiModelProperty(value = "执行结果：成功、失败")
    private String result;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;
}
