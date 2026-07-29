package org.jeecg.modules.bems.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceStaticDataVo {

    private String type;

    private Long deviceId;

    private Long configId;

    private String label;

    private String valueType;

    private String valueData;

    private String value;
}
