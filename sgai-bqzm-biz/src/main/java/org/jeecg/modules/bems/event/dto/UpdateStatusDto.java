package org.jeecg.modules.bems.event.dto;

import lombok.Data;

@Data
public class UpdateStatusDto {

    /**
     * 事件id
     */
    private String id;

    /**
     * 事件状态
     */
    private String status;

}
