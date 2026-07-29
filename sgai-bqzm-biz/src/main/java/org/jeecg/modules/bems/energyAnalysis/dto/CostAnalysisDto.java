package org.jeecg.modules.bems.energyAnalysis.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class CostAnalysisDto {

    @DateTimeFormat(pattern="yyyy-MM-dd")
    private LocalDate date;

    /**
     * 类别。电：electricity；水：water；热：heating
     */
    private String category;

    private Long categoryId;

    /**
     * 成本中心id
     */
    private Long costCenterId;
}
