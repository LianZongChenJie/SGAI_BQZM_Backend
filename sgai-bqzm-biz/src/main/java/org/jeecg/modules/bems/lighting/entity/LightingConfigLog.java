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
 * 配置日志表
 */
@Data
@TableName("lighting_config_log")
@ApiModel(value = "配置日志对象", description = "照明配置日志")
public class LightingConfigLog {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 操作时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "操作时间")
    private LocalDateTime operTime;

    /**
     * 操作类型：新增、修改、删除
     */
    @ApiModelProperty(value = "操作类型：新增、修改、删除")
    private String operType;

    /**
     * 操作模块：场景配置、定时控制、告警配置等
     */
    @ApiModelProperty(value = "操作模块：场景配置、定时控制、告警配置等")
    private String operModule;

    /**
     * 操作对象类型
     */
    @ApiModelProperty(value = "操作对象类型")
    private String targetType;

    /**
     * 操作对象ID
     */
    @ApiModelProperty(value = "操作对象ID")
    private Long targetId;

    /**
     * 操作对象名称
     */
    @ApiModelProperty(value = "操作对象名称")
    private String targetName;

    /**
     * 操作内容
     */
    @ApiModelProperty(value = "操作内容")
    private String operContent;

    /**
     * 操作人账号
     */
    @ApiModelProperty(value = "操作人账号")
    private String operBy;

    /**
     * 操作人姓名
     */
    @ApiModelProperty(value = "操作人姓名")
    private String operName;

    /**
     * IP地址
     */
    @ApiModelProperty(value = "IP地址")
    private String ipAddress;
}
