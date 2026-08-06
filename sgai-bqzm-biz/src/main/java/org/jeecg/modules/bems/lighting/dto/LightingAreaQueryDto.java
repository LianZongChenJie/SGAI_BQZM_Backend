package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;

@Data
public class LightingAreaQueryDto {

    private Integer pageNo = 1;

    private Integer pageSize = 10;

    private String relName;

    private String areaName;

    private String space;

    /**
     * 所属片区ID
     */
    private Long districtId;
}
