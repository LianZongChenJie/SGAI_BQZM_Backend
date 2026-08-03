package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 照明场景明细
 * 场景下的一个控制目标：某个区域或回路的开/关，独立存储于 lighting_scene_detail 表
 */
@Data
@TableName("lighting_scene_detail")
@ApiModel(value = "照明场景明细对象", description = "场景下的控制目标（区域/回路的开或关）")
public class LightingSceneDetail {

    /**
     * 主键（雪花ID，JSON 序列化为字符串避免前端精度丢失）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 场景id（雪花ID，JSON 序列化为字符串避免前端精度丢失）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "场景ID")
    private Long sceneId;

    /**
     * 关联类型。区域、回路
     */
    @ApiModelProperty(value = "关联类型：区域、回路")
    private String relType;

    /**
     * 关联id（区域id或回路id）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "关联ID（区域ID或回路ID）")
    private Long relId;

    /**
     * 名称（冗余存储，展示用）
     */
    @ApiModelProperty(value = "名称")
    private String relName;

    /**
     * 操作类型。开启、关闭
     */
    @ApiModelProperty(value = "操作类型：开启、关闭")
    private String operationType;

    /**
     * 排序字段，升序排列
     */
    @ApiModelProperty(value = "排序")
    private Long sort;
}
