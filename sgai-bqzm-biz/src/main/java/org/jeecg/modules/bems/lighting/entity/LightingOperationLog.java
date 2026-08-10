package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 照明-控制记录
 */
@Data
@TableName("lighting_operation_log")
public class LightingOperationLog {

    /**
     * 关联类型：区域
     */
    public static final String REL_TYPE_AREA = "区域";
    /**
     * 关联类型：回路
     */
    public static final String REL_TYPE_CIRCUIT = "回路";

    /**
     * 日志类型：一键控制
     */
    public static final String LOG_TYPE_ONE_KEY = "一键控制";
    /**
     * 日志类型：场景
     */
    public static final String LOG_TYPE_SCENE = "场景";
    /**
     * 日志类型：节目
     */
    public static final String LOG_TYPE_PROGRAM = "节目";
    /**
     * 日志类型：定时任务
     */
    public static final String LOG_TYPE_PLAN = "定时任务";
    /**
     * 日志类型：区域
     */
    public static final String LOG_TYPE_AREA = "区域";
    /**
     * 日志类型：回路
     */
    public static final String LOG_TYPE_CIRCUIT = "回路";

    /**
     * 操作类型（operatorType）：手动（用户直接操作）
     */
    public static final String OPERATOR_TYPE_MANUAL = "手动";
    /**
     * 操作类型（operatorType）：定时（计划到点自动执行）
     */
    public static final String OPERATOR_TYPE_PLAN = "定时";
    /**
     * 操作类型（operatorType）：场景（场景一键执行）
     */
    public static final String OPERATOR_TYPE_SCENE = "场景";

    /**
     * 主键（雪花ID，JSON 序列化为字符串避免前端精度丢失）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 日志类型：场景、定时任务、区域、回路
     */
    private String logType;

    /**
     * 父日志ID（顶层日志为null）
     */
    private Long parentId;

    /**
     * 关联类型，区域：1、回路：2
     */
    private String relType;
    /**
     * 关联id
     */
    private Long relId;
    /**
     * 名称
     */
    private String name;
    /**
     * 操作类型
     */
    private String operationType;
    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 操作人
     */
    private String operationBy;

    /**
     * 开灯时间
     */
    private String openTime;

    /**
     * 关灯时间
     */
    private String closeTime;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 操作类型：手动、定时、场景
     */
    private String operatorType;

    /**
     * 子日志列表（非表字段，详情查询用）
     */
    @TableField(exist = false)
    private List<LightingOperationLog> children;

}
