package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 片区树节点（第一层）：片区信息及其下的区域树
 * 片区 -> 区域 -> 箱子 三级树结构
 */
@Data
@ApiModel(value = "片区树节点", description = "片区-区域-箱子 树结构的第一层（片区）")
public class BoxTreeVo {

    @ApiModelProperty(value = "片区ID")
    private Long id;

    @ApiModelProperty(value = "片区名称")
    private String districtName;

    @ApiModelProperty(value = "该片区下的区域列表")
    private List<BoxTreeAreaVo> areas;
}
