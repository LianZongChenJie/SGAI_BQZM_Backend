package org.jeecg.modules.bems.mdm.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.mdm.entity.Device;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeviceDto extends Device {

    private String categoryIds;

    private String spaceIds;

    private String nameOrCode;

    /**
     * 楼控设备是否已绑定点位,绑定：1；未绑定：0
     */
    private Boolean associatedPoint;
}
