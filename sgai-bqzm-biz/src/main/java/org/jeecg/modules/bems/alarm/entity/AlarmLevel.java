package org.jeecg.modules.bems.alarm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

/**
 * 告警等级
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("alarm_level")
public class AlarmLevel extends BaseEntity {

    /**
     * 状态。启用
     */
    public static final String STATUS_ENABLE = "1";

    /**
     * 状态。禁用
     */
    public static final String STATUS_DISABLE = "0";

    /**
     * 等级名称
     */
    private String alarmLevelName;

    /**
     * 等级编号
     */
    private String alarmLevelCode;

    /**
     * 颜色
     */
    private String alarmLevelColor;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态。启用：1；禁用：0
     */
    private String status;
}
