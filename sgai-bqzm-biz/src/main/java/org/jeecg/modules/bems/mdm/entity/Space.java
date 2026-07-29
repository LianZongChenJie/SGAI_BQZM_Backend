package org.jeecg.modules.bems.mdm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @Description: 空间位置
 * @Author: jeecg-boot
 * @Date:   2025-02-20
 * @Version: V1.0
 */
@Data
@TableName("space")
@ApiModel(value="space对象", description="空间位置")
public class Space implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键*/
	@TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键")
    private java.lang.Long id;
	/**创建人*/
    @ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
	/**更新人*/
    @ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
	/**更新日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;
	/**所属部门*/
    @ApiModelProperty(value = "所属部门")
    private java.lang.String sysOrgCode;
	/**父级节点*/
	@Excel(name = "父级节点", width = 15)
    @ApiModelProperty(value = "父级节点")
    private java.lang.Long pid;
	/**是否有子节点*/
	@Excel(name = "是否有子节点", width = 15, dicCode = "yn")
	@Dict(dicCode = "yn")
    @ApiModelProperty(value = "是否有子节点")
    private java.lang.String hasChild;
	/**名称*/
	@Excel(name = "名称", width = 15)
    @ApiModelProperty(value = "名称")
    private java.lang.String spaceName;
	/**排序字段*/
	@Excel(name = "排序字段", width = 15)
    @ApiModelProperty(value = "排序字段")
    private java.lang.Integer sort;
	/**备注*/
	@Excel(name = "备注", width = 15)
    @ApiModelProperty(value = "备注")
    private java.lang.String remark;
	/**空间全称*/
	@Excel(name = "空间全称", width = 15)
    @ApiModelProperty(value = "空间全称")
    private java.lang.String fullName;
	/**父级id*/
	@Excel(name = "父级id", width = 15)
    @ApiModelProperty(value = "父级id")
    private java.lang.String fullId;

    public static SelectTreeModel convert(Space space){
        if(space == null){
            return null;
        }
        SelectTreeModel res = new SelectTreeModel();
        res.setKey(space.getId().toString());
        res.setTitle(space.getSpaceName());
        res.setParentId(space.getPid().toString());
        res.setValue(space.getFullName());
        return res;
    }
}
