package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.modules.bems.permission.annotation.DataPermissionField;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 照明区域
 */
@Data
@TableName("lighting_area")
@ApiModel(value = "照明区域对象", description = "照明区域/地块")
public class LightingArea {

    /**
     * 主键
     */
    @TableId(type= IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 空间。金安桥：1；一高炉：2；
     */
    @DataPermissionField(type = RoleDataPermission.TYPE_LIGHTING, value = "space")
    @ApiModelProperty(value = "空间编码")
    private String space;

    /**
     * 空间名称
     */
    @ApiModelProperty(value = "空间名称")
    private String spaceName;

    /**
     * 区域名称
     */
    @ApiModelProperty(value = "区域名称")
    private String areaName;

    /**
     * 区域编码
     */
    @ApiModelProperty(value = "区域编码")
    private String areaCode;

    /**
     * 状态：
     */
    @ApiModelProperty(value = "状态")
    private String status;

    /**
     * 上次操作时间
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "上次操作时间")
    private LocalDateTime lastOperationTime;

    /**
     * 上次操作人
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "上次操作人")
    private String lastOperationBy;

    /**
     * 1:区域回路、2:建筑回路
     */
    @ApiModelProperty(value = "类型：1-区域回路、2-建筑回路")
    private String type;
    /**
     * 位置信息
     */
    @ApiModelProperty(value = "位置信息")
    private String location;
    /**
     * 监控地址
     */
    @ApiModelProperty(value = "监控地址")
    private String monitorAdr;
    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

    /**
     * 场景启动时间，单位：秒
     */
    @ApiModelProperty(value = "场景启动时间（秒）")
    private Long allDuration;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "开灯时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "关灯时间")
    private LocalDateTime closingTime;

    /**
     * 场景开启码
     */
    @ApiModelProperty(value = "场景开启码")
    private String openCode;

    /**
     * 场景关闭码
     */
    @ApiModelProperty(value = "场景关闭码")
    private String closeCode;

    /**
     * 关联名称
     */
    @ApiModelProperty(value = "关联名称")
    private String relName;

    /**
     * 排序字段，升序排列
     */
    @ApiModelProperty(value = "排序")
    private Long sort;

    /**
     * 经度
     */
    @ApiModelProperty(value = "经度")
    private Double longitude;

    /**
     * 纬度
     */
    @ApiModelProperty(value = "纬度")
    private Double latitude;

    /**
     * 回路数
     */
    @ApiModelProperty(value = "回路数")
    private Integer circuitCount;

    /**
     * 在线数
     */
    @ApiModelProperty(value = "在线数")
    private Integer onlineCount;

    /**
     * 今日用电量（kWh）
     */
    @ApiModelProperty(value = "今日用电量（kWh）")
    private Double todayEnergy;

    /**
     * 累计用电量（kWh）
     */
    @ApiModelProperty(value = "累计用电量（kWh）")
    private Double totalEnergy;

    /**
     * 地图层级
     */
    @ApiModelProperty(value = "地图层级")
    private Integer mapLevel;
}
