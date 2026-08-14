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
 * 照明分钟电量读数表
 * MQ 消息 DataType=6（电量）时，把每分钟的累计电量读数落库
 */
@Data
@TableName("lighting_energy_read")
@ApiModel(value = "照明分钟电量读数", description = "MQ DataType=6 的每分钟累计电量读数")
public class LightingEnergyRead {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 网关编号（GatewayCode，如 12）
     */
    @ApiModelProperty(value = "网关编号（GatewayCode）")
    private String gatewayCode;

    /**
     * 回路编号（CircuitCode，为空表示网关级总表读数）
     */
    @ApiModelProperty(value = "回路编号（为空表示网关级总表）")
    private String circuitCode;

    /**
     * 区域ID（解析到回路/区域时回填）
     */
    @ApiModelProperty(value = "区域ID")
    private Long areaId;

    /**
     * 区域编码（area_code）
     */
    @ApiModelProperty(value = "区域编码")
    private String areaCode;

    /**
     * 空间编码（space）
     */
    @ApiModelProperty(value = "空间编码")
    private String space;

    /**
     * 累计电量（kWh）
     */
    @ApiModelProperty(value = "累计电量（kWh）")
    private BigDecimal value;

    /**
     * 读数时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "读数时间")
    private LocalDateTime readTime;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
