package org.jeecg.modules.bems.dto;

import lombok.Data;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeviceDataFindDto {

    private Long deviceId;

    private String deviceName;

    private String deviceCode;

    private Long categoryId;

    private Long spaceId;

    private String spaceIds;

    private String categoryIds;

    private List<Long> spaceIdList;

    private List<Long> categoryIdList;

    /**
     * 运行状态
     */
    private String runState;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateTime;

    /**
     * 负值异常
     * 空值异常
     * 全量数据
     */
    private String abnormalType;

    /**
     * 转换为整数，四舍五入
     */
    private String convertInteger;

    private int pageNo = 1;

    private int pageSize = 10;

    public Device convertToDevice() {
        Device device = new Device();
        device.setDeviceName(deviceName);
        device.setDeviceCode(deviceCode);
        device.setCategoryId(categoryId);
        device.setSpaceId(spaceId);
        device.setRunState(runState);
        device.setPageNo(pageNo);
        device.setPageSize(pageSize);
        return device;
    }

}
