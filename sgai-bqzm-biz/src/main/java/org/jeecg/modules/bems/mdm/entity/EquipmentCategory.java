package org.jeecg.modules.bems.mdm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.bems.entity.BaseEntity;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.io.Serializable;

/**
 * @Description: 设备类别
 * @Author: jeecg-boot
 * @Date:   2025-02-20
 * @Version: V1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("equipment_category")
@ApiModel(value="equipment_category对象", description="设备类别")
public class EquipmentCategory extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 类型。仪表：1；
     */
    public static final String TYPE_MEASURING = "1";
    /**
     * 类型。设备：2；
     */
    public static final String TYPE_EQUIPMENT = "2";

    /**
     * 类型。仪表：1；设备：2；
     */
    private String type;
	/**父级节点*/
	@Excel(name = "父级节点", width = 15)
    @ApiModelProperty(value = "父级节点")
    private java.lang.Long pid;
	/**是否有子节点*/
	@Excel(name = "是否有子节点", width = 15, dicCode = "yn")
	@Dict(dicCode = "yn")
    @ApiModelProperty(value = "是否有子节点")
    private java.lang.String hasChild;
	/**类别名称*/
	@Excel(name = "类别名称", width = 15)
    @ApiModelProperty(value = "类别名称")
    private java.lang.String categoryName;
	/**排序*/
	@Excel(name = "排序", width = 15)
    @ApiModelProperty(value = "排序")
    private java.lang.Integer sort;
	/**备注*/
	@Excel(name = "备注", width = 15)
    @ApiModelProperty(value = "备注")
    private java.lang.String remark;
	/**全称*/
	@Excel(name = "全称", width = 15)
    @ApiModelProperty(value = "全称")
    private java.lang.String fullName;
	/**父级id*/
	@Excel(name = "父级id", width = 15)
    @ApiModelProperty(value = "父级id")
    private java.lang.String fullId;

    public static SelectTreeModel convert(EquipmentCategory category){
        if(category == null){
            return null;
        }
        SelectTreeModel model = new SelectTreeModel();
        model.setTitle(category.getCategoryName());
        model.setKey(category.getId().toString());
        model.setParentId(category.getPid().toString());
        model.setValue(category.getFullName());
        return model;
    }
}
