package org.jeecg.modules.bems.energyAnalysis.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 碳排放分析dto
 */
@Data
public class CarbonEmissionDto {

    private String type;

    /**
     * 日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 对比日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate compareDate;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 对比年份
     */
    private Integer compareYear;

    /**
     * 对比月份
     */
    private Integer compareMonth;

    /**
     * 计量规则点位id集合
     */
    private String pointIds;
}
