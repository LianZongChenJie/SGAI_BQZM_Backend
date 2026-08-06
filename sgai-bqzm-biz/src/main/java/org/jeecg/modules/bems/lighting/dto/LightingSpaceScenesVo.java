package org.jeecg.modules.bems.lighting.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingScene;

import java.util.List;

/**
 * 按空间查询：该空间下的所有场景（含明细）和所有回路信息
 */
@Data
@ApiModel(value = "空间场景回路信息", description = "某个空间下的所有场景和所有回路")
public class LightingSpaceScenesVo {

    /**
     * 空间id（对应 lighting_area.space）
     */
    @ApiModelProperty(value = "空间ID")
    private String spaceId;

    /**
     * 空间名称
     */
    @ApiModelProperty(value = "空间名称")
    private String spaceName;

    /**
     * 该空间下的所有回路（含 areaName/spaceName 回填）
     */
    @ApiModelProperty(value = "空间下所有回路")
    private List<LightingCircuit> circuits;

    /**
     * 该空间下的所有场景（含 details 明细）
     */
    @ApiModelProperty(value = "空间下所有场景（含明细）")
    private List<LightingScene> scenes;
}
