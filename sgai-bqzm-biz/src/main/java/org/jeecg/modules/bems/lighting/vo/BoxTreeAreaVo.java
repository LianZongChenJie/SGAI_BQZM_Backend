package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 区域树节点（第二层）：片区下的区域
 */
@Data
@ApiModel(value = "区域树节点", description = "片区-区域-箱子 树结构的第二层（区域）")
public class BoxTreeAreaVo {

    @ApiModelProperty(value = "区域ID")
    private Long id;

    @ApiModelProperty(value = "区域名称")
    private String areaName;

    @ApiModelProperty(value = "状态：开启、关闭")
    private String status;

    @ApiModelProperty(value = "是否报警（报警/正常）")
    private String alarmFlag;

    @ApiModelProperty(value = "报警数量")
    private Integer alarmCount;

    @ApiModelProperty(value = "是否有箱子（有/无）")
    private String hasBox;

    @ApiModelProperty(value = "该区域下的箱子列表")
    private List<BoxTreeBoxVo> boxes;
}
