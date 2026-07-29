package org.jeecg.modules.bems.mdm.dto;

import lombok.Data;

@Data
public class DeviceAttributeControlDto {

    /**
     * 设备属性id
     */
    private Long deviceAttributeId;

    /**
     * 设备值
     */
    private String value;
}
