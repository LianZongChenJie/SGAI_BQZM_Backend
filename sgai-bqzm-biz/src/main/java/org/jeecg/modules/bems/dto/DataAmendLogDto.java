package org.jeecg.modules.bems.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.DataAmendLog;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class DataAmendLogDto extends DataAmendLog {

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备编号
     */
    private String deviceCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endTime;

    public static DataAmendLogDto convert(DataAmendLog log){
        DataAmendLogDto res = new DataAmendLogDto();
        res.setId(log.getId());
        res.setDeviceId(log.getDeviceId());
        res.setHourDataId(log.getHourDataId());
        res.setTime(log.getTime());
        res.setStartValue(log.getStartValue());
        res.setEndValue(log.getEndValue());
        res.setComputeValue(log.getComputeValue());
        res.setOriginalValue(log.getOriginalValue());
        res.setValue(log.getValue());
        res.setUpdateTime(log.getUpdateTime());
        res.setUpdateBy(log.getUpdateBy());
        res.setStartTime(log.getTime());
        res.setEndTime(log.getTime().plusHours(1));
        return res;
    }
}
