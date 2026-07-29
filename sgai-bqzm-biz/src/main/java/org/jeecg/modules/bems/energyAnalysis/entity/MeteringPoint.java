package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;
import org.jeecg.modules.bems.permission.annotation.DataPermissionField;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;

import java.io.Serializable;

/**
 * @Description: 计量点位
 * @Author: jeecg-boot
 * @Date: 2025-02-26
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("metering_point")
@ApiModel(value = "metering_point对象", description = "计量点位")
public class MeteringPoint extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableField(exist = false)
    public static final Long ROOT_ID = 0L;


    @ApiModelProperty(value = "类型。数据字典：energy_flow_type")
    public String type;

    @ApiModelProperty(value = "节点名称")
    private String nodeName;

    @ApiModelProperty(value = "父节点")
    private Long parentId;


    @ApiModelProperty(value = "排序")
    private Integer sort;

    @ApiModelProperty(value = "节点编码")
    private String nodeCode;

    @ApiModelProperty(value = "设备类别")
    @DataPermissionField(type = RoleDataPermission.TYPE_CATEGORY, value = "category_id")
    private Long categoryId;

    @ApiModelProperty(value = "空间位置")
    @DataPermissionField(type = RoleDataPermission.TYPE_SPACE, value = "space_id")
    private Long spaceId;

    @ApiModelProperty(value = "计量单位")
    private Long meteringUnit;

    @ApiModelProperty(value = "公式")
    private String formula;

    /**
     * 真实公式，将formula中引用的其他点位解析成对应的公式
     */
    private String trueFormula;
}
