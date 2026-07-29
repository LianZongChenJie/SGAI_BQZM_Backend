package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;

@Data
public class LightingPlanQueryDto {

    /**
     * 关联类型。区域、回路
     */
    private String relType;
    /**
     * 控制时间，起始
     */
    private String startTime;

    /**
     * 控制时间，结束
     */
    private String endTime;

    private Integer pageNo = 1;

    private Integer pageSize = 10;
}
