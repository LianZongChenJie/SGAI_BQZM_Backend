package org.jeecg.modules.bems.dataBoard.dto;

import lombok.Data;

@Data
public class EnergyConsumptionStatisticsConfig {

    /**
     * 能耗统计配置名称
     */
    private String name;

    /**
     * 能耗统计配置内容-计量规则点位id
     */
    private Long pointId;
}
