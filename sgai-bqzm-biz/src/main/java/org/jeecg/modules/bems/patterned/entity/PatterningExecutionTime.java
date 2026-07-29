package org.jeecg.modules.bems.patterned.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "patterning_execution_time")
public class PatterningExecutionTime extends BaseEntity {

    private final static DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final static DateTimeFormatter time_formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 模式化管理策略. */
    private Long patterningId;
    /** 策略起始日期.yyyy-MM-dd */
    private LocalDate beginDate;
    /** 策略执行时间. HH:mm:ss*/
    private LocalTime beginTime;
    /** 周策略执行日【1,2,3->.周一周二周三】 */
    private String enabledWeek;
    /** 结束日期.yyyy-MM-dd */
    private LocalDate endDate;

    /**
     * 版本号
     */
    private String version;

}
