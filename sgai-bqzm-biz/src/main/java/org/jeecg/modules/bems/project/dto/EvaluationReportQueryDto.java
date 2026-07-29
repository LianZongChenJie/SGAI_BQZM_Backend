package org.jeecg.modules.bems.project.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class EvaluationReportQueryDto {

    /**
     * 计量规则点位id
     */
    private Long pointId;

    /**
     * 评估开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 评估结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 基准开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime baseStartTime;

    /**
     * 基准结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime baseEndTime;

    /**
     * 颗粒度：小时:hour、日:day、月:month、年:year
     */
    private String dateType;

}
