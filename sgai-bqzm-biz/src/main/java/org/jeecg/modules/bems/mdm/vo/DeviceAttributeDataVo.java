package org.jeecg.modules.bems.mdm.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceAttributeDataVo {

    private Long deviceId;

    private Long configId;

    private String label;

    private String code;

    private String value;

    private String valueType;


    public static DeviceAttributeDataVo build(Long deviceId,Long configId,String label,String code,String value){
        return new DeviceAttributeDataVo(deviceId,configId,label,code,value,"show");
    }

}
