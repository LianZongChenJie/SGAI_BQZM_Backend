package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Data
@TableName("lighting_plan_execution_time")
public class LightingPlanExecutionTime {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 主键（雪花ID，JSON 序列化为字符串避免前端精度丢失）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 计划id（雪花ID，JSON 序列化为字符串避免前端精度丢失）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long planId;

    private String executionTime;

    private String startDate;

    private String endDate;

    /**
     * 周几执行
     */
    private String enabledWeek;

    /**
     * 版本号
     */
    private String version;

    public LocalDate getStartLocalDate(){
        if(StringUtils.isEmpty(startDate)){
            return null;
        }
        return LocalDate.parse(startDate, DATE_FORMATTER);
    }

    public LocalDate getEndLocalDate(){
        if(StringUtils.isEmpty(endDate)){
            return null;
        }
        return LocalDate.parse(endDate, DATE_FORMATTER);
    }

    public LocalTime getExecutionLocalTime(){
        if(StringUtils.isEmpty(executionTime)){
            return null;
        }
        return LocalTime.parse(executionTime, TIME_FORMATTER);
    }
}
