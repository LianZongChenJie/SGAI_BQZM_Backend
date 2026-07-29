package org.jeecg.modules.bems.alarm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

/**
 * 告警类别
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("alarm_category")
public class AlarmCategory extends BaseEntity {

    /**
     * 状态。启用
     */
    public static final String STATUS_ENABLE = "1";
    /**
     * 状态。禁用
     */
    public static final String STATUS_DISABLE = "0";

    /**
     * 告警类别名称
     */
    private String alarmCategoryName;

    /**
     * 告警类别编码
     */
    private String alarmCategoryCode;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态。启用：1；禁用：0
     */
    private String status;
}
