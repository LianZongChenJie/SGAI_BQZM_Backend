package org.jeecg.modules.bems.alarm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 告警记录
 */
@EqualsAndHashCode(callSuper = true)
@TableName("alarm_record")
@Data
public class AlarmRecord extends BaseEntity {

    /**
     * 告警状态：待处理
     */
    public static final String ALARM_STATUS_UNTREATED = "1";

    /**
     * 告警状态：已完成
     */
    public static final String ALARM_STATUS_TREATED = "2";
    /**
     * 告警状态：处理中（转单）
     */
    public static final String ALARM_STATUS_EVENT = "3";

    /**
     * 告警规则id
     */
    private Long alarmRuleId;

    /**
     * 告警规则点位id
     */
    private Long alarmRulePointId;

    /**
     * 设备id
     */
    private Long deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 设备类别id
     */
    private Long deviceCategoryId;

    /**
     * 告警内容
     */
    private String alarmContent;

    /**
     * 告警时间
     */
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime alarmTime;

    /**
     * 告警类别id
     */
    private Long alarmCategoryId;

    /**
     * 告警类别名称
     */
    private String alarmCategoryName;
    /**
     * 告警级别id
     */
    private Long alarmLevelId;
    /**
     * 告警级别名称
     */
    private String alarmLevelName;

    /**
     * 点位id
     */
    private Long pointId;

    private String pointName;

    /**
     * 时间粒度
     */
    private String timeGranularity;

    /**
     * 点位值（告警值）
     */
    private String value;

    /**
     * 条件值（阈值）
     */
    private String conditionValue;

    /**
     * 条件
     */
    private String operator;

    /**
     * 负责人
     */
    private Long chargePerson;

    /**
     * 负责人
     */
    private String chargePersonName;

    /**
     * 状态。待处理：1；已完成：2；处理中（转单）：3
     */
    private String alarmStatus;

    /**
     * 告警级别颜色
     */
    private String alarmLevelColor;

    /**
     * 事件id
     */
    private String eventId;
}
