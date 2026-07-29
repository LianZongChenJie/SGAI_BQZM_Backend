package org.jeecg.modules.bems.event.dto;

import lombok.Data;

/**
 * 创建事件
 */
@Data
public class CreateEventDto {
    private String eventCode = "开始-待处理";

    private BusinessDataDto businessData;
}
