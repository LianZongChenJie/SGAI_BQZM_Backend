package org.jeecg.modules.bems.lighting.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 照明节目分页查询参数
 */
@Data
@ApiModel(value = "照明节目查询参数", description = "照明节目分页查询参数")
public class LightingProgramQueryDto {

    /**
     * 节目名称（模糊匹配）
     */
    @ApiModelProperty(value = "节目名称（模糊匹配）")
    private String programName;

    /**
     * 泛光节目ID（精确匹配）
     */
    @ApiModelProperty(value = "泛光节目ID（精确匹配）")
    private String groupId;

    /**
     * 所属区域（对应 lighting_area.space，精确匹配）
     */
    @ApiModelProperty(value = "所属区域（对应 lighting_area.space，精确匹配，如 1金安桥/2一高炉）")
    private String space;

    /**
     * 状态：启用、禁用
     */
    @ApiModelProperty(value = "状态：启用、禁用")
    private String status;

    /**
     * 标签ID（精确匹配）
     */
    @ApiModelProperty(value = "标签ID（精确匹配）")
    private String tagId;

    /**
     * 标签名称（模糊匹配）
     */
    @ApiModelProperty(value = "标签名称（模糊匹配）")
    private String tagName;

    @ApiModelProperty(value = "页码", example = "1")
    private Integer pageNo = 1;

    @ApiModelProperty(value = "每页条数", example = "10")
    private Integer pageSize = 10;
}
