package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

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

    /** 持续验证状态：无需（计划未开启持续验证） */
    public static final String VERIFY_NONE = "无需";
    /** 持续验证状态：待验证（已登记，等待到期执行复查） */
    public static final String VERIFY_PENDING = "待验证";
    /** 持续验证状态：已完成（已复查并按需补发） */
    public static final String VERIFY_DONE = "已完成";
    /** 持续验证状态：已跳过（计划不存在等异常，未执行复查） */
    public static final String VERIFY_SKIPPED = "已跳过";

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
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    /** MQ 消息消费时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date consumeTime;

    /** 备注（失败原因等） */
    private String remark;

    /** 持续验证状态：无需/待验证/已完成/已跳过 */
    private String verifyStatus;

    /** 验证执行时刻（计划执行时刻 + 延迟分钟） */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date verifyTime;

    /** 验证/补发结果详情（只记摘要：第N/M轮、复查数、补发数、跳过原因；补发明细在控制日志里） */
    private String verifyResult;

    /** 持续验证已执行轮次（含当前轮；0/null=尚未执行）。总轮次取 business_config: plan:verify:times */
    private Integer verifyCount;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新人 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 组织机构编码 */
    private String sysOrgCode;

}