package org.jeecg.modules.bems.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 定时任务配置
 * 
 * 支持两种模式：
 * 1. 灯光控制模式（controlType=AREA/CIRCUIT）: 定时开灯/关灯
 * 2. 通用反射模式（controlType=null）: 通过 beanName + methodName 调用任意 Spring Bean 方法
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("schedule_job")
@ApiModel(value = "ScheduleJob对象", description = "定时任务配置")
public class ScheduleJob extends BaseEntity {

    /** 任务名称 */
    @ApiModelProperty(value = "任务名称")
    private String jobName;

    /** 
     * 控制类型
     * AREA   - 区域灯光控制
     * CIRCUIT - 回路灯光控制
     * null   - 通用反射模式（通过 beanName + methodName 调用）
     */
    @ApiModelProperty(value = "控制类型：AREA-区域控制 CIRCUIT-回路控制 null-通用反射")
    private String controlType;

    /** 目标ID（区域ID或回路ID，控制类型为 AREA/CIRCUIT 时必填） */
    @ApiModelProperty(value = "目标ID（区域ID或回路ID）")
    private Long targetId;

    /** 
     * 操作类型
     * 灯光控制时: OPEN-开启 CLOSE-关闭
     * 通用反射时: 可自定义
     */
    @ApiModelProperty(value = "操作类型：OPEN-开启 CLOSE-关闭")
    private String operationType;

    /** Spring Bean 名称（通用反射模式时使用） */
    @ApiModelProperty(value = "Spring Bean 名称")
    private String beanName;

    /** 执行方法名（通用反射模式时使用，必须是无参方法） */
    @ApiModelProperty(value = "执行方法名")
    private String methodName;

    /** cron 表达式 */
    @ApiModelProperty(value = "cron 表达式")
    private String cronExpression;

    /** 任务参数（可选，通用反射模式时使用） */
    @ApiModelProperty(value = "任务参数")
    private String params;

    /** 状态：0-禁用 1-启用 */
    @ApiModelProperty(value = "状态：0-禁用 1-启用")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 上次执行时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "上次执行时间")
    private java.util.Date lastRunTime;

    /** 下次执行时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "下次执行时间")
    private java.util.Date nextRunTime;
}
