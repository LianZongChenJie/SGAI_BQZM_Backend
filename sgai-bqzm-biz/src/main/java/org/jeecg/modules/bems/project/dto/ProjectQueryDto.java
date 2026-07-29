package org.jeecg.modules.bems.project.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectQueryDto {

    /**
     * 项目名称
     */
    private String projectName;
    /**
     * 立项时间查询：开始时间
     */
    private LocalDateTime startDate;

    /**
     * 立项时间查询：结束时间
     */
    private LocalDateTime endDate;

    private Long pageNo = 1L;

    private Long pageSize = 10L;

}
