package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 照明计划执行日志（MQ 发送/消费追踪）
 * 用于日历展示计划执行成功/执行失败：成功与否看 MQ 消息是否被消费
 */
@Data
@TableName("lighting_plan_execute_log")
public class LightingPlanExecuteLog {

    /** 状态：待消费（已发送延迟消息，尚未被消费） */
    public static final String STATUS_PENDING = "待消费";
    /** 状态：执行成功（MQ 消息已被消费，计划执行完成） */
    public static final String STATUS_SUCCESS = "执行成功";
    /** 状态：执行失败（MQ 消息消费异常 / 计划执行失败） */
    public static final String STATUS_FAIL = "执行失败";

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划ID（lighting_plan.id） */
    private Long planId;

    /** 计划名称 */
    private String planName;

    /** 计划版本号（执行时间配置版本） */
    private String version;

    /** 计划执行日期 yyyy-MM-dd */
    private String executeDate;

    /** 计划执行时间 HH:mm:ss */
    private String executionTime;

    /** 状态：待消费/执行成功/执行失败 */
    private String status;

    /** MQ 消息发送时间 */
    private LocalDateTime sendTime;

    /** MQ 消息消费时间 */
    private LocalDateTime consumeTime;

    /** 备注（失败原因等） */
    private String remark;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新人 */
    private String updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 组织机构编码 */
    private String sysOrgCode;

}
