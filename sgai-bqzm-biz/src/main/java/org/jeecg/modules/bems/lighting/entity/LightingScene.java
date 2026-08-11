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
     * 类别：一键开关（禁止删除）
     */
    public static final String CATEGORY_ONE_CLICK_SWITCH = "一键开关";

    /**
     * 类别：节目（禁止删除）
     */
    public static final String CATEGORY_PROGRAM = "节目";

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
     * 类别：节目类型、普通类型等（区分场景类别，如泛光节目/其他）
     */
    @ApiModelProperty(value = "类别：节目类型、普通类型等")
    private String category;

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
     * 标签ID（冗余存储，用于场景分组/筛选展示）
     */
    @ApiModelProperty(value = "标签ID")
    private String tagId;

    /**
     * 标签名称（冗余存储，用于场景分组/筛选展示）
     */
    @ApiModelProperty(value = "标签名称")
    private String tagName;

    /**
     * 节目ID集合（逗号分隔，关联 lighting_program.id，即节目表的节目）
     * 新建场景时可选择节目表的节目，也可选择区域/回路信息；控制时按节目 groupId 发泛光节目MQ
     */
    @ApiModelProperty(value = "节目ID集合（逗号分隔，关联 lighting_program.id，如 1,2,3）")
    private String programSceneIds;

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
