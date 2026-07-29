package org.jeecg.modules.bems.project.dto;

import lombok.Data;

@Data
public class EnergyMarketData {

    /**
     * 评估时间
     */
    private String evaluationTime;
    /**
     * 项目实施后能耗
     */
    private String energyConsumption;

    /**
     * 对标时间
     */
    private String baseTime;

    /**
     * 项目实施前能耗
     */
    private String baseEnergyConsumption;

    /**
     * 节能量
     */
    private String energySavings;
}
