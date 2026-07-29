package org.jeecg.modules.bems.alarm.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.alarm.entity.AlarmRecord;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class AlarmRecordDto extends AlarmRecord {

    /**
     * 开始日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDateTime;

    /**
     * 结束日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDateTime;

    private String deviceIds;
}
