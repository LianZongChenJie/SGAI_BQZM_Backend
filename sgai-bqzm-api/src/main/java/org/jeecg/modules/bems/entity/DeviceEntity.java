package org.jeecg.modules.bems.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceEntity {

    private Long id;

    private String deviceCode;

    private Long categoryId;
}
