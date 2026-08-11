package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

/**
 * 照明节目（泛光节目）
 * 从 lighting_scene 拆分独立存储于 lighting_program 表，承载泛光节目ID(groupId) 等节目信息。
 * 场景（lighting_scene.program_scene_ids）引用本表 id，控制时按 groupId 发泛光节目MQ。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("lighting_program")
@ApiModel(value = "照明节目对象", description = "照明节目（泛光节目），独立存储于 lighting_program 表")
public class LightingProgram extends BaseEntity {

    public static final String STATUS_ENABLE = "启用";
    public static final String STATUS_DISABLE = "禁用";

    public static final String OPERATION_TYPE_OPEN = "开启";
    public static final String OPERATION_TYPE_CLOSE = "关闭";

    /**
     * 主键（雪花ID，JSON 序列化为字符串避免前端精度丢失）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 节目名称
     */
    @ApiModelProperty(value = "节目名称")
    private String programName;

    /**
     * 泛光节目ID（关联泛光总控系统的节目）
     * 控制时按此ID发泛光节目MQ给小程序（onOff：1开2关）
     */
    @ApiModelProperty(value = "泛光节目ID")
    private String groupId;

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
     * 所属区域（对应 lighting_area.space，如 1金安桥/2一高炉/903北区，用于节目按区域筛选）
     */
    @ApiModelProperty(value = "所属区域（对应 lighting_area.space，如 1金安桥/2一高炉）")
    private String space;

    /**
     * 标签ID（冗余存储，用于节目分组/筛选展示）
     */
    @ApiModelProperty(value = "标签ID")
    private String tagId;

    /**
     * 标签名称（冗余存储，用于节目分组/筛选展示）
     */
    @ApiModelProperty(value = "标签名称")
    private String tagName;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;
}
