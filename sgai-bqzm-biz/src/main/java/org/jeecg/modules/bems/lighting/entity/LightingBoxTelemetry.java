package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import org.jeecg.modules.bems.permission.annotation.DataPermissionField;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;
import java.io.Serializable;
import java.util.Date;

/**
 * 箱子(电箱)遥测最新快照表（每次推送更新一次）
 * 数据来源：MQ 消息 DataType=7（交流电压/电流/功率/电量）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "箱子遥测快照", description = "每个箱子一条最新记录")
@TableName("lighting_box_telemetry")
public class LightingBoxTelemetry implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "箱子(网关)编号")
    private String gatewayCode;

    @ApiModelProperty(value = "所属区域id")
    private Long areaId;

    @ApiModelProperty(value = "区域名称")
    private String areaName;

    @DataPermissionField(type = RoleDataPermission.TYPE_DISTRICT, value = "district_id")
    @ApiModelProperty(value = "所属片区id")
    private Long districtId;

    @ApiModelProperty(value = "片区名称（非表字段，查询时动态填充）")
    @TableField(exist = false)
    private String districtName;

    /** 三相电压 (V) */
    @ApiModelProperty(value = "交流电压1(A相)")
    private Double voltageA;
    @ApiModelProperty(value = "交流电压2(B相)")
    private Double voltageB;
    @ApiModelProperty(value = "交流电压3(C相)")
    private Double voltageC;

    /** 三相电流 (A) */
    @ApiModelProperty(value = "交流电流1(A相)")
    private Double currentA;
    @ApiModelProperty(value = "交流电流2(B相)")
    private Double currentB;
    @ApiModelProperty(value = "交流电流3(C相)")
    private Double currentC;

    /** 功率 */
    @ApiModelProperty(value = "有功功率(kW)")
    private Double activePower;
    @ApiModelProperty(value = "无功功率(kVar)")
    private Double reactivePower;
    @ApiModelProperty(value = "视在功率(kVA)")
    private Double apparentPower;
    @ApiModelProperty(value = "功率因数")
    private Double powerFactor;

    /** 累积电量 */
    @ApiModelProperty(value = "累积电量(kWh)")
    private Double totalEnergy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "采集时间")
    private Date collectTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    private String sysOrgCode;
}