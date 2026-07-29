package org.jeecg.modules.bems.bc.dto;

import lombok.Data;

@Data
public class BuildingControlPointDto {

    /**
     * 楼控点位id
     */
    private Long pointId;

    private Integer pageNo = 1;

    private Integer pageSize = 10;
}
