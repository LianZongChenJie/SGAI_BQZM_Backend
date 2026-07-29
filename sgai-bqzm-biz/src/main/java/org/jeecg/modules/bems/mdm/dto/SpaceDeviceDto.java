package org.jeecg.modules.bems.mdm.dto;

import lombok.Data;
import org.jeecg.modules.bems.mdm.entity.Device;

import java.util.List;

@Data
public class SpaceDeviceDto {
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 空间名称
     */
    private String spaceName;
    /**
     * 子空间
     */
    private List<SpaceDeviceDto> children;
    /**
     * 当前空间下设备信息
     */
    private List<Device> devices;

}
