package org.jeecg.modules.bems.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DeviceDataWaterVo {

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

    @Excel(name = "设备类型",orderNum = "2")
    private String categoryName;
    @Excel(name = "空间位置",orderNum = "3")
    private String spaceName;

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
    @Excel(name = "起始时间",exportFormat = "yyyy-MM-dd",orderNum = "4")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间",exportFormat = "yyyy-MM-dd",orderNum = "6")
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

    public static DeviceDataWaterVo convert(DeviceDataVo device){
        if(device == null){
            return null;
        }
        DeviceDataWaterVo res = new DeviceDataWaterVo();
        res.setDeviceCode(device.getDeviceCode());
        res.setDeviceName(device.getDeviceName());
        res.setCategoryName(device.getCategoryName());
        res.setSpaceName(device.getSpaceName());
        res.setStartValue(device.getStartValue());
        res.setEndValue(device.getEndValue());
        res.setStartTime(device.getStartTime());
        res.setEndTime(device.getEndTime());
        res.setValue(device.getValue());
        res.setRunState(device.getRunState());
        return res;
    }
}
