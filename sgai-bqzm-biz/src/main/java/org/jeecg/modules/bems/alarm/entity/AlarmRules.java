package org.jeecg.modules.bems.alarm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

import java.util.List;

/**
 * 告警规则
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("alarm_rules")
public class AlarmRules extends BaseEntity {

    /**
     * 瞬时值
     */
    public static final String POINT_TYPE_INSTANT = "instant";
    /**
     * 累计值
     */
    public static final String POINT_TYPE_ACCUMULATE = "accumulate";

    /**
     * 计量规则累计值
     */
    public static final String POINT_TYPE_VIRTUAL = "virtual";

    /**
     * 启用
     */
    public static final String ENABLED_STATUS_ENABLE = "1";
    /**
     * 禁用
     */
    public static final String ENABLED_STATUS_DISABLE = "0";

    /** 规则编号 */
    private String ruleCode;
    /** 规则名称 */
    private String ruleName;
    /** 报警类别 */
    private Long alarmCategoryId;
    /** 报警类别名称 */
    private String alarmCategoryName;
    /** 报警等级 */
    private Long alarmLevelId;
    /** 报警等级名称 */
    private String alarmLevelName;

    /**
     * 频率
     */
    private Integer frequency;

    /**
     * 频率单位。秒：s；分钟：m；小时：h；天：d
     */
    private String frequencyUnit;

    /**
     * 报警点位类型
     */
    private String pointType;

    /** 通知用户id */
    private String noticeUser;

    /**
     * 通知方式，多个用逗号分隔。平台：platform、短信：sms、邮件：email
     */
    private String noticeWay;

    /**
     * 启用状态，启用：1；禁用：0
     */
    private String enabledStatus;

    /**
     * 告警级别颜色
     */
    private String alarmLevelColor;

    @TableField(exist = false)
    private List<AlarmRulePoint> points;
}
