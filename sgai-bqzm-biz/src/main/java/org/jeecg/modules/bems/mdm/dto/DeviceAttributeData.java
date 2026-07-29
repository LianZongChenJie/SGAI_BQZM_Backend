package org.jeecg.modules.bems.mdm.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeviceAttributeData {

    private String EquipmentCode;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime Timestamp;

    private List<AttributeData> Data;
}
