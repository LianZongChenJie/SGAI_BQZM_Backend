package org.jeecg.modules.bems.energyAnalysis.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class MeteringPointChatDto {

    /**
     * 计量点位id
     */
    private Long pointId;

    /**
     * 日期类型；年：year;月：month;日：day
     */
    private String dateType;

    /**
     * 基准开始日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate baseStartDate;

    /**
     * 基准结束日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate baseEndDate;

    /**
     * 分析开始日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 分析结束日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 涨幅
     */
    private String increase;

    /**
     * 超过涨幅提示词
     */
    private String increaseContent;
}
