package org.jeecg.modules.bems.energyAnalysis.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class EnergyFlowDiagramDto {

    /**
     * 能流图分类
     */
    private String type;

    /**
     * 日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 开始日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
