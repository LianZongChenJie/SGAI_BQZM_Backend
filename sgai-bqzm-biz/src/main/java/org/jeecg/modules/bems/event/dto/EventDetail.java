package org.jeecg.modules.bems.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDetail {
    private Event event;

    private List<EventOperateRecord> operateRecords;
}
