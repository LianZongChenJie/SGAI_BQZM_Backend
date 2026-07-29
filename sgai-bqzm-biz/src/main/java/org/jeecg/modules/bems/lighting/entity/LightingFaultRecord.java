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
 * 故障记录表
 */
@Data
@TableName("lighting_fault_record")
@ApiModel(value = "故障记录对象", description = "照明故障记录")
public class LightingFaultRecord {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 故障类型：通信故障、通讯异常、过压故障、离线故障、照明失效
     */
    @ApiModelProperty(value = "故障类型：通信故障、通讯异常、过压故障、离线故障、照明失效")
    private String faultType;

    /**
     * 故障等级：紧急、重要、一般
     */
    @ApiModelProperty(value = "故障等级：紧急、重要、一般")
    private String faultLevel;

    /**
     * 设备ID
     */
    @ApiModelProperty(value = "设备ID")
    private Long deviceId;

    /**
     * 设备名称
     */
    @ApiModelProperty(value = "设备名称")
    private String deviceName;

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
     * 故障内容
     */
    @ApiModelProperty(value = "故障内容")
    private String faultContent;

    /**
     * 故障发生时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "故障发生时间")
    private LocalDateTime faultTime;

    /**
     * 恢复时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "恢复时间")
    private LocalDateTime recoverTime;

    /**
     * 故障状态：未处理、处理中、已恢复
     */
    @ApiModelProperty(value = "故障状态：未处理、处理中、已恢复")
    private String faultStatus;

    /**
     * 处理人
     */
    @ApiModelProperty(value = "处理人")
    private String handlePerson;

    /**
     * 处理时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "处理时间")
    private LocalDateTime handleTime;

    /**
     * 处理描述
     */
    @ApiModelProperty(value = "处理描述")
    private String handleDesc;

    /**
     * 故障持续时间（分钟）
     */
    @ApiModelProperty(value = "故障持续时间（分钟）")
    private Long duration;

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
