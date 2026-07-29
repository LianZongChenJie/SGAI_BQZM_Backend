package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;

@Data
public class LightingCircuitQueryDto {
    /**
     * 区域id
     */
    private Long areaId;

    private Integer pageNo = 1;

    private Integer pageSize = 10;
}
