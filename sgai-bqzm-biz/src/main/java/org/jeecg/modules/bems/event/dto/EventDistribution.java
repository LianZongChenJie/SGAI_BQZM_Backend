package org.jeecg.modules.bems.event.dto;

import lombok.Data;

/**
 * 事件、工单分布
 */
@Data
public class EventDistribution {

    /**
     * 名称
     */
    private String name;
    /**
     * 数量
     */
    private Long value;
}
