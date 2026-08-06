package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;

@Data
public class LightingCircuitQueryDto {
    /**
     * 区域id
     */
    private Long areaId;

    /**
     * 所属片区ID
     */
    private Long districtId;

    private Integer pageNo = 1;

    private Integer pageSize = 10;
}
