package org.jeecg.modules.bems.energyAnalysis.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MeteringPointDataDto {

    /**
     * 节点id
     */
    private String energyFlowDiagramIds;

    /**
     * 日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate day;

    /**
     * 小时
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime hour;

}
