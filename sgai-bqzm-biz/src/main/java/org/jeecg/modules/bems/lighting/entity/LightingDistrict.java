package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 照明片区（与区域 lighting_area 一对多：一个片区下包含多个区域）
 */
@Data
@TableName("lighting_district")
@ApiModel(value = "照明片区对象", description = "照明片区信息")
public class LightingDistrict {

    /**
     * 主键
     */
    @TableId
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 片区名称
     */
    @ApiModelProperty(value = "片区名称")
    private String districtName;

    /**
     * 片区编码
     */
    @ApiModelProperty(value = "片区编码")
    private String districtCode;

    /**
     * 状态：启用、停用
     */
    @ApiModelProperty(value = "状态：启用、停用")
    private String status;

    /**
     * 排序
     */
    @ApiModelProperty(value = "排序")
    private Integer sort;

    /**
     * 空间编码
     */
    @ApiModelProperty(value = "空间编码")
    private String space;

    /**
     * 空间名称
     */
    @ApiModelProperty(value = "空间名称")
    private String spaceName;

    /**
     * 位置信息
     */
    @ApiModelProperty(value = "位置信息")
    private String location;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

    /**
     * 类型
     */
    @ApiModelProperty(value = "类型")
    private String type;

    /**
     * 累计运行时长，单位：秒
     */
    @ApiModelProperty(value = "累计运行时长（秒）")
    private Long allDuration;

    /**
     * 回路数
     */
    @ApiModelProperty(value = "回路数")
    private Integer circuitCount;

    /**
     * 在线数
     */
    @ApiModelProperty(value = "在线数")
    private Integer onlineCount;

    /**
     * 地图层级
     */
    @ApiModelProperty(value = "地图层级")
    private Integer mapLevel;

    /**
     * 下属区域空间编码集合（逗号分隔，如 1,2,3，关联 lighting_area.space）
     */
    @ApiModelProperty(value = "下属区域空间编码集合（逗号分隔，如 1,2,3）")
    private String spaceIds;
}
