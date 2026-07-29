package org.jeecg.modules.bems.homePage.dto;

import lombok.Data;

/**
 * 用能安全数据展示
 */
@Data
public class EnergyUseSafetyDataDto {

    /**
     * 名称
     */
    private String name;

    /**
     * 额定负荷
     */
    private String ratedLoad;

    /**
     * 当前负荷
     */
    private String currentLoad;

    /**
     * 负荷率
     */
    private String loadRate;
}
