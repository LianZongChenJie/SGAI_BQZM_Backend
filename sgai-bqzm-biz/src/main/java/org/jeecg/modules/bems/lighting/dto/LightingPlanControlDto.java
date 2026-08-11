package org.jeecg.modules.bems.lighting.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 灯光批量控制参数（按计划列表信息全开/全关）
 */
@Data
@ApiModel(value = "灯光批量控制参数")
public class LightingPlanControlDto {

    /**
     * 关联类型：区域、回路
     */
    @ApiModelProperty(value = "关联类型：区域、回路")
    private String relType;

    /**
     * 关联ID，多个以英文逗号分隔
     */
    @ApiModelProperty(value = "关联ID，多个以英文逗号分隔，如 1,2,3")
    private String relIds;

    /**
     * 操作类型：开启、关闭（兼容 OPEN/CLOSE）
     */
    @ApiModelProperty(value = "操作类型：开启、关闭（兼容 OPEN/CLOSE）")
    private String operationType;

    /**
     * 场景ID（可选）。传了则控制后只同步该场景的状态；
     * 不传则自动反查：明细包含这些目标的场景全部同步状态。
     */
    @ApiModelProperty(value = "场景ID（可选），控制后同步该场景开关状态")
    private Long sceneId;

    /**
     * 节目ID集合（可选，英文逗号分隔，关联 lighting_program.id）。
     * 只传节目信息时（relType/relIds 可为空），按节目 groupId 发泛光节目MQ，仅执行节目。
     */
    @ApiModelProperty(value = "节目ID集合（可选），只传节目时仅执行节目")
    private String programSceneIds;
}
