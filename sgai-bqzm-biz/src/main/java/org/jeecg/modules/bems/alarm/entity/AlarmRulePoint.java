package org.jeecg.modules.bems.alarm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

/**
 * 告警规则设备点位
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("alarm_rule_point")
public class AlarmRulePoint extends BaseEntity {

    /**
     * 时间粒度：小时
     */
    public static final String TIME_GRANULARITY_HOUR = "hour";
    /**
     * 时间粒度：日
     */
    public static final String TIME_GRANULARITY_DAY = "day";
    /**
     * 时间粒度：月
     */
    public static final String TIME_GRANULARITY_MONTH = "month";
    /**
     * 时间粒度：年
     */
    public static final String TIME_GRANULARITY_YEAR = "year";

    /**
     * 规则id
     */
    private Long alarmRuleId;

    /**
     * 设备id
     */
    private Long deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 点位id
     */
    private Long pointId;

    /**
     * 点位名称
     */
    private String pointName;

    /**
     * 时间粒度，小时：hour、日：day、月：month、年：year
     */
    private String timeGranularity;

    /**
     * 条件运算符
     */
    private String operator;

    /**
     * 条件值
     */
    private String conditionValue;

    public String getTimeGranularityStr(){
        if (timeGranularity == null) {
            return "";
        }
        switch (timeGranularity){
            case TIME_GRANULARITY_HOUR:
                return "小时";
            case TIME_GRANULARITY_DAY:
                return "日";
            case TIME_GRANULARITY_MONTH:
                return "月";
            case TIME_GRANULARITY_YEAR:
                return "年";
            default:
                return "";
        }
    }
}
