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
 * @Description: 折标煤系数管理
 * @Author: jeecg-boot
 * @Date:   2025-03-05
 * @Version: V1.0
 */
@Data
@TableName("standard_coal_coefficient")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="standard_coal_coefficient对象", description="折标煤系数管理")
public class StandardCoalCoefficient implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private java.lang.String id;
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
	/**能源介质*/
	@Excel(name = "能源介质", width = 15)
    @ApiModelProperty(value = "能源介质")
    private java.lang.String energyMedium;
	/**单位*/
	@Excel(name = "单位", width = 15)
    @ApiModelProperty(value = "单位")
    private java.lang.String unit;
	/**当量折算系数*/
	@Excel(name = "当量折算系数", width = 15)
    @ApiModelProperty(value = "当量折算系数")
    private java.lang.String eccsc;
	/**等价折算系数*/
	@Excel(name = "等价折算系数", width = 15)
    @ApiModelProperty(value = "等价折算系数")
    private java.lang.String ecf;
	/**排序*/
	@Excel(name = "排序", width = 15)
    @ApiModelProperty(value = "排序")
    private java.lang.Integer sort;
	/**说明*/
	@Excel(name = "说明", width = 15)
    @ApiModelProperty(value = "说明")
    private java.lang.String remark;
}
