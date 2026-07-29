package org.jeecg.modules.bems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @Description: 能介管理
 * @Author: jeecg-boot
 * @Date:   2025-02-25
 * @Version: V1.0
 */
@Data
@TableName("energy_medium_manage")
@ApiModel(value="energy_medium_manage对象", description="能介管理")
public class EnergyMediumManage implements Serializable {
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
	/**能介编码*/
	@Excel(name = "能介编码", width = 15)
    @ApiModelProperty(value = "能介编码")
    private java.lang.String code;
	/**能介名称*/
	@Excel(name = "能介名称", width = 15)
    @ApiModelProperty(value = "能介名称")
    private java.lang.String name;
	/**标准单位*/
	@Excel(name = "标准单位", width = 15)
    @ApiModelProperty(value = "标准单位")
    private java.lang.Long standardUnit;
	/**排序*/
	@Excel(name = "排序", width = 15)
    @ApiModelProperty(value = "排序")
    private java.lang.Integer sort;
	/**分时计量*/
	@Excel(name = "分时计量", width = 15)
    @ApiModelProperty(value = "分时计量")
    private java.lang.String timeSharing;
	/**说明*/
	@Excel(name = "说明", width = 15)
    @ApiModelProperty(value = "说明")
    private java.lang.String remark;
}
