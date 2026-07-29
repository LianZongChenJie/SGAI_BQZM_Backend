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
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 采集管理-规则标准
 * @Author: jeecg-boot
 * @Date:   2025-02-19
 * @Version: V1.0
 */
@Data
@TableName("gather_rule_config")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="gather_rule_config对象", description="采集管理-规则标准")
public class GatherRuleConfig implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;
	/**创建人*/
    @ApiModelProperty(value = "创建人")
    private String createBy;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private Date createTime;
	/**更新人*/
    @ApiModelProperty(value = "更新人")
    private String updateBy;
	/**更新日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private Date updateTime;
	/**所属部门*/
    @ApiModelProperty(value = "所属部门")
    private String sysOrgCode;
	/**网关编号*/
	@Excel(name = "网关编号", width = 15)
    @ApiModelProperty(value = "网关编号")
    private String gatewayCode;
	/**网关名称*/
	@Excel(name = "网关名称", width = 15)
    @ApiModelProperty(value = "网关名称")
    private String gatewayName;
	/**网关类型*/
	@Excel(name = "网关类型", width = 15, dicCode = "rule_gatewayType")
	@Dict(dicCode = "rule_gatewayType")
    @ApiModelProperty(value = "网关类型")
    private String gatewayType;
	/**安装位置*/
	@Excel(name = "安装位置", width = 15)
    @ApiModelProperty(value = "安装位置")
    private Long installAddr;
	/**ip*/
	@Excel(name = "ip", width = 15)
    @ApiModelProperty(value = "ip")
    private String ip;
	/**通讯协议*/
	@Excel(name = "通讯协议", width = 15, dicCode = "rule_protocol")
	@Dict(dicCode = "rule_protocol")
    @ApiModelProperty(value = "通讯协议")
    private String protocol;
	/**状态*/
	@Excel(name = "状态", width = 15, dicCode = "rule_state")
	@Dict(dicCode = "rule_state")
    @ApiModelProperty(value = "状态")
    private String state;
	/**最后采集时间*/
	@Excel(name = "最后采集时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最后采集时间")
    private Date lastCollectionTime;
	/**采集频率/s*/
	@Excel(name = "采集频率/s", width = 15)
    @ApiModelProperty(value = "采集频率/s")
    private Integer frequency;
}
