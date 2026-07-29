package org.jeecg.modules.bems.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DeviceDataVo {

    /**
     * 设备id
     */
    private Long deviceId;

    /**
     * 设备编号
     */
    @Excel(name = "设备编号",orderNum = "0")
    private String deviceCode;
    /**
     * 设备名称
     */
    @Excel(name = "设备名称",orderNum = "1")
    private String deviceName;

    private Long categoryId;

    private Long spaceId;

    @Excel(name = "设备类型",orderNum = "2")
    private String categoryName;
    @Excel(name = "空间位置",orderNum = "3")
    private String spaceName;

    /**
     * 单位
     */
    private String unit;
    /**
     * 起始值
     */
    @Excel(name = "起始值",orderNum = "5")
    private BigDecimal startValue;

    /**
     * 结束值
     */
    @Excel(name = "结束值",orderNum = "7")
    private BigDecimal endValue;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "起始时间",exportFormat = "yyyy-MM-dd HH:mm:ss",orderNum = "4")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间",exportFormat = "yyyy-MM-dd HH:mm:ss",orderNum = "6")
    private LocalDateTime endTime;

    /**
     * 计算值
     */
    @Excel(name = "计算值",orderNum = "8")
    private BigDecimal value;

    /**
     * 运行状态
     */
    @Excel(name = "运行状态",orderNum = "9")
    private String runState;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastGatherTime;

    /**
     * 电池电压
     */
    private String colVoltage;

    public static DeviceDataVo convert(Device device){
        if(device == null){
            return null;
        }
        DeviceDataVo res = new DeviceDataVo();
        res.setDeviceId(device.getId());
        res.setDeviceCode(device.getDeviceCode());
        res.setDeviceName(device.getDeviceName());
        res.setSpaceId(device.getSpaceId());
        res.setCategoryId(device.getCategoryId());
        res.setRunState(device.getRunState());
        res.setLastGatherTime(device.getLastGatherTime());
        return res;
    }

}
