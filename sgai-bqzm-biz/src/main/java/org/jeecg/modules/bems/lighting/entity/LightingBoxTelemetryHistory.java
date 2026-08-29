package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 箱子(电箱)遥测历史数据（每次推送保存一条）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "箱子遥测历史", description = "每次推送保存一条历史记录")
@TableName("lighting_box_telemetry_history")
public class LightingBoxTelemetryHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String gatewayCode;
    private Long areaId;
    private String areaName;
    private Long districtId;

    private Double voltageA;
    private Double voltageB;
    private Double voltageC;

    private Double currentA;
    private Double currentB;
    private Double currentC;

    private Double activePower;
    private Double reactivePower;
    private Double apparentPower;
    private Double powerFactor;

    private Double totalEnergy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date collectTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    private String sysOrgCode;
}