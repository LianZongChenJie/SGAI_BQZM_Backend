package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

import java.util.List;

/**
 * 照明场景
 * 场景是一组区域/回路开关目标的组合，不绑定定时任务，用于一键开关灯。
 * 独立存储于 lighting_scene 表。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("lighting_scene")
@ApiModel(value = "照明场景对象", description = "照明场景，包含多个区域/回路的开关控制目标")
public class LightingScene extends BaseEntity {

    public static final String REL_TYPE_AREA = "区域";

    public static final String REL_TYPE_CIRCUIT = "回路";

    public static final String OPERATION_TYPE_OPEN = "开启";
    public static final String OPERATION_TYPE_CLOSE = "关闭";

    public static final String STATUS_ENABLE = "启用";
    public static final String STATUS_DISABLE = "禁用";

    /**
     * 主键（雪花ID，JSON 序列化为字符串避免前端精度丢失）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 场景名称
     */
    @ApiModelProperty(value = "场景名称")
    private String sceneName;

    /**
     * 场景类型：普通场景、节日场景、应急场景
     */
    @ApiModelProperty(value = "场景类型：普通场景、节日场景、应急场景")
    private String sceneType;

    /**
     * 状态：启用、禁用
     */
    @ApiModelProperty(value = "状态：启用、禁用")
    private String status;

    /**
     * 排序字段，升序排列
     */
    @ApiModelProperty(value = "排序")
    private Long sort;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

    /**
     * 场景明细（非表字段）
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "场景明细列表")
    private List<LightingSceneDetail> details;

    /**
     * 目标数量（非表字段，列表展示用）
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "目标数量")
    private Integer detailCount;
}
