package org.jeecg.modules.bems.energyAnalysis.dto;

import lombok.Data;

@Data
public class MeasureRuleDto {

    private String type;

    private Long parentId;

    private int pageNo = 1;

    private int pageSize = 10;

}
