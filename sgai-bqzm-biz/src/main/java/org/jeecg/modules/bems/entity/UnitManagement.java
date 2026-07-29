package org.jeecg.modules.bems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @Description: 计量单位管理
 * @Author: jeecg-boot
 * @Date:   2025-02-25
 * @Version: V1.0
 */
@Data
@TableName("unit_management")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="unit_management对象", description="计量单位管理")
public class UnitManagement implements Serializable {
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
	/**单位代码*/
	@Excel(name = "单位代码", width = 15)
    @ApiModelProperty(value = "单位代码")
    private java.lang.String code;
	/**单位名称*/
	@Excel(name = "单位名称", width = 15)
    @ApiModelProperty(value = "单位名称")
    private java.lang.String name;
	/**英文名称*/
	@Excel(name = "英文名称", width = 15)
    @ApiModelProperty(value = "英文名称")
    private java.lang.String englishAme;
	/**排序*/
	@Excel(name = "排序", width = 15)
    @ApiModelProperty(value = "排序")
    private java.lang.Integer sort;
	/**说明*/
	@Excel(name = "说明", width = 15)
    @ApiModelProperty(value = "说明")
    private java.lang.String remark;
}
