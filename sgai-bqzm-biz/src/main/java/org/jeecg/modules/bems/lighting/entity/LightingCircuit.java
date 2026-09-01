package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 照明回路信息
 */
@Data
@TableName("lighting_circuit")
@ApiModel(value = "照明回路对象", description = "照明回路信息")
public class LightingCircuit {

    public static final String COMSTAT_ONLINE = "在线";

    public static final String COMSTAT_OFFLINE = "离线";

    public static final String STATUS_ON = "开启";

    public static final String STATUS_OFF = "关闭";

    public static final Map<String,String> STATUS_MAP = Map.of(
            "100", "开启",
            "0", "关闭"
    );

    @TableId
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 回路名称
     */
    @ApiModelProperty(value = "回路名称")
    private String circuitName;

    /**
     * 回路编号
     */
    @ApiModelProperty(value = "回路编号")
    private String circuitCode;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态：开启、关闭")
    private String status;

    /**
     * 所在区域
     */
    @ApiModelProperty(value = "区域ID")
    private Long areaId;

    /**
     * 区域编码
     */
    @ApiModelProperty(value = "区域编码")
    private String areaCode;

    /**
     * 回路开启时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "回路开启时间")
    private LocalDateTime startTime;

    /**
     * 回路关闭时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "回路关闭时间")
    private LocalDateTime closingTime;

    /**
     * 开启总时长
     */
    @ApiModelProperty(value = "开启总时长（秒）")
    private Long allDuration;

    /**
     * 操作人
     */
    @ApiModelProperty(value = "操作人")
    private String operatorBy;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "操作时间")
    private LocalDateTime operatorTime;

    /**
     * 区域名称
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "区域名称")
    private String areaName;

    /**
     * 通讯状态，在线、离线
     */
    @ApiModelProperty(value = "通讯状态：在线、离线")
    private String comstat;

    /**
     * 是否报警（报警/正常）
     */
    @ApiModelProperty(value = "是否报警（报警/正常）")
    private String alarmFlag;

    /**
     * 空间名称
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "空间名称")
    private String spaceName;

    /**
     * 额定功率（kW）
     */
    @ApiModelProperty(value = "额定功率（kW）")
    private Double ratedPower;

    /**
     * 当前功率（kW）
     */
    @ApiModelProperty(value = "当前功率（kW）")
    private Double currentPower;

    /**
     * 今日用电量（kWh）
     */
    @ApiModelProperty(value = "今日用电量（kWh）")
    private Double todayEnergy;

    /**
     * 累计运行时长（小时）
     */
    @ApiModelProperty(value = "累计运行时长（小时）")
    private Long totalRunTime;

    /**
     * 电压（V）
     */
    @ApiModelProperty(value = "电压（V）")
    private Double voltage;

    /**
     * 实时电流（A）
     */
    @ApiModelProperty(value = "电流（A）")
    private Double electricCurrent;
    /**
     * 额定电流（A）
     */
    @ApiModelProperty(value = "额定电流（A）")
    private Double ratedElectricCurrent;

    /**
     * 功率因数
     */
    @ApiModelProperty(value = "功率因数")
    private Double powerFactor;

    /**
     * 最后在线时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最后在线时间")
    private LocalDateTime lastOnlineTime;

    /**
     * 设备型号
     */
    @ApiModelProperty(value = "设备型号")
    private String deviceModel;

    /**
     * 安装日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "安装日期")
    private LocalDateTime installDate;
}
