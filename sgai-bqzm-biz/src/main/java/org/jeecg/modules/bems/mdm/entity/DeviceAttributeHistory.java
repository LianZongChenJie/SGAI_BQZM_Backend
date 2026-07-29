package org.jeecg.modules.bems.mdm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 设备属性历史值
 */
@Data
@TableName("device_attribute_history")
public class DeviceAttributeHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备id
     */
    private Long deviceId;

    /**
     * 设备属性id
     */
    private Long attributeId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectionTime;

    /**
     * 属性值
     */
    private String value;

}
