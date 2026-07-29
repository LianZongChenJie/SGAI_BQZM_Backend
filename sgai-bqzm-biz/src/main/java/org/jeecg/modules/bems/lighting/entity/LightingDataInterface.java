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
 * 数据采集接口表
 */
@Data
@TableName("lighting_data_interface")
@ApiModel(value = "数据采集接口对象", description = "照明数据采集接口")
public class LightingDataInterface {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 接口名称
     */
    @ApiModelProperty(value = "接口名称")
    private String interfaceName;

    /**
     * 厂商：西门子、施耐德、ABB、华为PLC、海康威视
     */
    @ApiModelProperty(value = "厂商：西门子、施耐德、ABB、华为PLC、海康威视")
    private String manufacturer;

    /**
     * 协议类型：OPC UA、Modbus TCP、MQTT、HTTP API、SDK
     */
    @ApiModelProperty(value = "协议类型：OPC UA、Modbus TCP、MQTT、HTTP API、SDK")
    private String protocolType;

    /**
     * 接口地址
     */
    @ApiModelProperty(value = "接口地址")
    private String interfaceAddress;

    /**
     * 数据类型：实时数据/开关量、实时数据/模拟量、开关量/模拟量、视频/监控
     */
    @ApiModelProperty(value = "数据类型：实时数据/开关量、实时数据/模拟量、开关量/模拟量、视频/监控")
    private String dataType;

    /**
     * 状态：正常、离线、异常
     */
    @ApiModelProperty(value = "状态：正常、离线、异常")
    private String status;

    /**
     * 最后同步时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最后同步时间")
    private LocalDateTime lastSyncTime;

    /**
     * 同步频率（秒）
     */
    @ApiModelProperty(value = "同步频率（秒）")
    private Integer syncFrequency;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

    /**
     * 排序
     */
    @ApiModelProperty(value = "排序")
    private Integer sort;

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
