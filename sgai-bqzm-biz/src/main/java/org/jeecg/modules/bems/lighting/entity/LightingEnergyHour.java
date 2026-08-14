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
 * 照明小时电量统计表
 * 每小时整点由定时任务聚合上一小时（末条读数 - 上小时末条读数），供能耗统计接口使用
 */
@Data
@TableName("lighting_energy_hour")
@ApiModel(value = "照明小时电量统计", description = "整点聚合的每小时用电量")
public class LightingEnergyHour {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 统计日期（yyyy-MM-dd）
     */
    @ApiModelProperty(value = "统计日期（yyyy-MM-dd）")
    private String statDate;

    /**
     * 统计小时（0-23，该小时内的用电量）
     */
    @ApiModelProperty(value = "统计小时（0-23）")
    private Integer statHour;

    /**
     * 网关编号
     */
    @ApiModelProperty(value = "网关编号")
    private String gatewayCode;

    /**
     * 回路编号（为空表示网关级总表）
     */
    @ApiModelProperty(value = "回路编号（为空表示网关级总表）")
    private String circuitCode;

    /**
     * 区域ID
     */
    @ApiModelProperty(value = "区域ID")
    private Long areaId;

    /**
     * 区域编码
     */
    @ApiModelProperty(value = "区域编码")
    private String areaCode;

    /**
     * 空间编码
     */
    @ApiModelProperty(value = "空间编码")
    private String space;

    /**
     * 该小时用电量（kWh）
     */
    @ApiModelProperty(value = "该小时用电量（kWh）")
    private BigDecimal energy;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
