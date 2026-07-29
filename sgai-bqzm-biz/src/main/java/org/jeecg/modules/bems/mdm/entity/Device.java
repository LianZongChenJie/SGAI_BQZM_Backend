package org.jeecg.modules.bems.mdm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;
import org.jeecg.modules.bems.permission.annotation.DataPermissionField;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description: 设备基础信息
 * @Author: jeecg-boot
 * @Date:   2025-02-20
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device")
@ApiModel(value="device对象", description="设备基础信息")
public class Device extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 设备分类。仪表：1；
     */
    public static final String DEVICE_TYPE_MEASURING = "1";
    /**
     * 设备分类。设备：2；
     */
    public static final String DEVICE_TYPE_EQUIPMENT = "2";

    @ApiModelProperty(value = "设备编号")
    private String deviceCode;

    @ApiModelProperty(value = "设备名称")
    private String deviceName;

    @ApiModelProperty(value = "设备类别id")
    @DataPermissionField(type = RoleDataPermission.TYPE_CATEGORY, value = "category_id")
    private Long categoryId;

    @ApiModelProperty(value = "空间位置id")
    @DataPermissionField(type = RoleDataPermission.TYPE_SPACE, value = "space_id")
    private Long spaceId;

    /**
     * 空间位置
     */
    @TableField(exist = false)
    private String spaceName;

    /**
     * 倍率
     */
    @ApiModelProperty(value = "倍率")
    private BigDecimal magnification;

    /**
     * 自动算法。启动：1；禁用：0
     */
    @ApiModelProperty(value = "自动算法")
    private String automaticAlgorithm;

    /**
     * 设备模型
     */
    @ApiModelProperty(value = "设备模型id")
    private Long modelId;

    @ApiModelProperty(value = "排序")
    private Integer sort;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "运行状态")
    private String runState;

    /**
     * 设备分类。仪表：1；设备：2；
     */
    private String deviceType;

    /**
     * 最后采集时间
     */
    private LocalDateTime lastGatherTime;

    @TableField(exist = false)
    private List<DeviceAttribute> attributes;

    /**
     * 电池电压
     */
    @TableField(exist = false)
    private String voltage;

}
