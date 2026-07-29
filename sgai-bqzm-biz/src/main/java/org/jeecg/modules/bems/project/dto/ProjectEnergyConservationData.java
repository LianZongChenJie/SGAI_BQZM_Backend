package org.jeecg.modules.bems.project.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProjectEnergyConservationData {
    /**
     * 节能总量
     */
    private BigDecimal total;

    /**
     * 各类型节能量
     * key：类型名称，value：节能量
     */
    private List<ProjectTypeEnergyConservationData> list;

}
