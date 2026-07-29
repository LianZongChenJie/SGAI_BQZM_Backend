package org.jeecg.modules.bems.mdm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceAttributeDataDto {

    private String attributeCode;

    private String value;

    private LocalDateTime gatherTime;
}
