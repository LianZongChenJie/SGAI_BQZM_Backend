package org.jeecg.modules.bems.lighting.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.modules.bems.lighting.entity.LightingSceneDetail;

import java.util.List;

/**
 * 照明场景新增/编辑入参
 * 兼容两种传参方式：
 * 1) 前端现用格式（与照明计划 plan/add 一致）：planName + relType + relIds + operationType
 * 2) 明细列表格式：sceneName + details[{relType, relId, operationType}]
 */
@Data
@ApiModel(value = "照明场景参数", description = "照明场景新增/编辑入参，兼容 planName+relType+relIds+operationType 或 sceneName+details 两种格式")
public class LightingSceneDto {

    /**
     * 场景id（编辑时必填）
     */
    @ApiModelProperty(value = "场景id（编辑时必填）")
    private Long id;

    /**
     * 场景名称（格式2使用）
     */
    @ApiModelProperty(value = "场景名称（与 planName 二选一）")
    private String sceneName;

    /**
     * 场景名称（前端现用，与 sceneName 二选一）
     */
    @ApiModelProperty(value = "场景名称（前端现用格式，等价于 sceneName）")
    private String planName;

    /**
     * 关联类型。区域、回路
     */
    @ApiModelProperty(value = "关联类型：区域、回路")
    private String relType;

    /**
     * 关联id，多个以英文逗号分隔
     */
    @ApiModelProperty(value = "关联ID，多个以英文逗号分隔，如 1,2,3")
    private String relIds;

    /**
     * 操作类型。开启、关闭
     */
    @ApiModelProperty(value = "操作类型：开启、关闭")
    private String operationType;

    /**
     * 场景类型：普通场景、节日场景、应急场景
     */
    @ApiModelProperty(value = "场景类型：普通场景、节日场景、应急场景")
    private String sceneType;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

    /**
     * 排序字段，升序排列
     */
    @ApiModelProperty(value = "排序")
    private Long sort;

    /**
     * 标签ID（可选，用于场景分组/筛选）
     */
    @ApiModelProperty(value = "标签ID（可选，用于场景分组/筛选）")
    private String tagId;

    /**
     * 标签名称（可选，用于场景分组/筛选展示）
     */
    @ApiModelProperty(value = "标签名称（可选，用于场景分组/筛选展示）")
    private String tagName;

    /**
     * 场景明细（格式2使用，与 relIds 二选一）
     */
    @ApiModelProperty(value = "场景明细列表（与 relType+relIds 二选一）")
    private List<LightingSceneDetail> details;
}
