package org.jeecg.modules.bems.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
import java.time.LocalDateTime;

/**
 * @Description: 项目管理
 * @Author: jeecg-boot
 * @Date:   2025-05-26
 * @Version: V1.0
 */
@Data
@TableName("project")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="project对象", description="项目管理")
public class Project implements Serializable{
    private static final long serialVersionUID = 1L;

    /**
     * 项目类型字典标识
     */
    public static final String PROJECT_TYPE_DICT_CODE = "project_type";

    /**主键*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private java.lang.String id;
	/**项目名称*/
	@Excel(name = "项目名称", width = 15)
    @ApiModelProperty(value = "项目名称")
    private java.lang.String projectName;
	/**立项时间*/
	@Excel(name = "立项时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "立项时间")
    private LocalDateTime projectEstablishmentTime;
	/**项目周期（单位可以根据实际情况确定，月*/
	@Excel(name = "项目周期（单位可以根据实际情况确定，月", width = 15)
    @ApiModelProperty(value = "项目周期（单位可以根据实际情况确定，月")
    private java.lang.Integer projectCycle;
	/**项目预算*/
	@Excel(name = "项目预算", width = 15)
    @ApiModelProperty(value = "项目预算")
    private java.math.BigDecimal projectBudget;
	/**项目主体*/
	@Excel(name = "项目主体", width = 15)
    @ApiModelProperty(value = "项目主体")
    private java.lang.String projectSubject;
    /**项目目标*/
    @Excel(name = "项目目标", width = 15)
    @ApiModelProperty(value = "项目目标")
    private java.lang.String projectGoal;
	/**项目文件（可存储文件相关信息或路径等）*/
	@Excel(name = "项目文件（可存储文件相关信息或路径等）", width = 15)
    @ApiModelProperty(value = "项目文件（可存储文件相关信息或路径等）")
    private java.lang.String projectFiles;
    /**
     * 关联计量规则点位id
     */
    private Long pointId;

    /**
     * 完整计量规则点位id
     */
    private String fullPointId;

    /**
     * 节能类型,对应数据字典
     */
    private String projectType;

    /**
     * 计量开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime measurementTime;

    /**
     * 计量规则点位名称
     */
    @TableField(exist = false)
    private String pointName;


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

    /**
     * 校验是否项目结束
     * @return 结束：true，未结束：false
     */
    public boolean isCompleted(){
        return this.getMeasurementTime() != null && !this.getMeasurementTime().isAfter(LocalDateTime.now());
    }
}
