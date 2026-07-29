package org.jeecg.modules.bems.homePage.dto;

import lombok.Data;

/**
 * 碳足迹数据
 */
@Data
public class CarbonFootprintDataDto {

    /**
     * 今日排放量
     */
    private String todayCarbonEmission;

    /**
     * 较前日
     */
    private String todayCarbonEmissionCompare;

    /**
     * 本周排放量
     */
    private String weekCarbonEmission;
    /**
     * 较上周
     */
    private String weekCarbonEmissionCompare;

    /**
     * 本月排放量
     */
    private String monthCarbonEmission;
    /**
     * 较上月
     */
    private String monthCarbonEmissionCompare;
    /**
     * 本季排放量
     */
    private String quarterCarbonEmission;
    /**
     * 较上季
     */
    private String quarterCarbonEmissionCompare;
    /**
     * 本年排放量
     */
    private String yearCarbonEmission;
    /**
     * 较上年
     */
    private String yearCarbonEmissionCompare;
}
