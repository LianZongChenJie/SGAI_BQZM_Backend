package org.jeecg.modules.bems.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceInfo {
    /**
     * 设备ID
     */
    private Long id;
    /**
     * 设备编号
     */
    private String deviceCode;
    /**
     * 设备名称
     */
    private String deviceName;
    /**
     * 设备所属类别名称（全称）
     */
    private String categoryName;
    /**
     * 设备所属空间名称（全称）
     */
    private String spaceName;

}
