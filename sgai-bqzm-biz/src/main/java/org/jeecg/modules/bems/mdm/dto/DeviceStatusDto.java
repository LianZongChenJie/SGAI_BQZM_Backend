package org.jeecg.modules.bems.mdm.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceStatusDto {

    /**
     * 设备编号
     */
    private String EquipmentCode;

    /**
     * online:在线；offline:离线；
     */
    private String OnlineStatus;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime Timestamp;

    public String getDeviceRunStatus(){
        switch(OnlineStatus){
            case "online":
                return "在线";
            case "offline":
                return "离线";
            default:
                return "未知";
        }
    }

}
