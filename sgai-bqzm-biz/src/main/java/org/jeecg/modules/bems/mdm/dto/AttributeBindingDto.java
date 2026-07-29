package org.jeecg.modules.bems.mdm.dto;

import lombok.Data;

/**
 * 设备属性绑定楼控点位信息
 */
@Data
public class AttributeBindingDto {

    /**
     * 设备属性id
     */
    private Long pointId;

    /**
     * 采集网关地址
     */
    private String gatewayAdr;

    /**
     * BACnet地址
     */
    private String bacnetAdr;
}
