package org.jeecg.modules.bems.patterned.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 设备属性值操作
 */
@Data
@AllArgsConstructor
public class DeviceAttributeOperationDto {

    private Long deviceId;

    private Long pointId;

    private String value;
}
