package org.jeecg.modules.bems.wd.dto;

import lombok.Data;

/**
 * 火警处理及时率、异常处理及时率、异常处置情况
 */
@Data
public class SituationStatisticDto {

    /**
     * 火警处理率（小数）
     */
    private String fireRate;

    /**
     * 故障处理率（小数）
     */
    private String faultRate;

    /**
     * 异常处理及时率（小数）
     */
    private String exceptionRate;

}
