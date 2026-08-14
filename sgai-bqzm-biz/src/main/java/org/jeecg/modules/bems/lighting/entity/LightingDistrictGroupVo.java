package org.jeecg.modules.bems.lighting.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 片区下分组-区域树（listByDistrict 返回结构）
 * 一个 vo 对应一个分组：groupName + 该分组下的区域列表
 */
@Data
@ApiModel(value = "照明片区-分组-区域树", description = "按片区查询的分组及其区域列表")
public class LightingDistrictGroupVo {

    /**
     * 片区ID
     */
    @ApiModelProperty(value = "片区ID")
    private Long districtId;

    /**
     * 片区名称
     */
    @ApiModelProperty(value = "片区名称")
    private String districtName;

    /**
     * 分组ID（稳定标识：districtId_groupName，分组无独立表时的唯一键）
     */
    @ApiModelProperty(value = "分组ID（districtId_groupName）")
    private String groupId;

    /**
     * 分组名称
     */
    @ApiModelProperty(value = "分组名称")
    private String groupName;

    /**
     * 分组下的区域列表
     */
    @ApiModelProperty(value = "分组下的区域列表")
    private List<LightingArea> areas;
}
