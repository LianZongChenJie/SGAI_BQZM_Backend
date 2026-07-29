package org.jeecg.modules.bems.homePage.dto;

import lombok.Data;

@Data
public class AlarmStatisticsDto {

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 总数
     */
    private Long total;

    /**
     * 未处理
     */
    private Long unprocessed;

    /**
     * 已处理
     */
    private Long processed;

    public AlarmStatisticsDto(String categoryName,Long total){
        this.categoryName = categoryName;
        this.total = total;
    }

    public AlarmStatisticsDto(Long categoryId,String categoryName){
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.total = 0L;
        this.unprocessed = 0L;
        this.processed = 0L;
    }
}
