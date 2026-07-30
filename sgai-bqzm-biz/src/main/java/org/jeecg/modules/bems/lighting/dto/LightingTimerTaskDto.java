package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;

@Data
public class LightingTimerTaskDto {

    private Long id;

    private String planName;

    private String relType;

    private String relIds;

    private String relNames;

    private String planType;

    private String cycleType;

    private String operationType;

    private String status;

    private String executionTime;

    private String startDate;

    private String endDate;

    private String enabledWeek;

    private String version;

    private Integer pageNo = 1;

    private Integer pageSize = 10;
}