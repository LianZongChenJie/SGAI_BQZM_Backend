package org.jeecg.modules.bems.mdm.dto;

import lombok.Data;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.time.format.DateTimeFormatter;

@Data
public class DeviceExportDto {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 设备名称
     */
    @Excel(name = "设备名称")
    private String deviceName;
    /**
     * 设备编号
     */
    @Excel(name = "设备编号")
    private String deviceCode;
    /**
     * 设备类型
     */
    @Excel(name = "设备类型")
    private String categoryName;

    private Long categoryId;
    /**
     * 设备位置
     */
    @Excel(name = "设备位置")
    private String spaceName;

    private Long spaceId;

    @Excel(name="备注")
    private String remark;

    /**
     * 设备运行状态
     */
    @Excel(name = "状态")
    private String runState;

    /**
     * 最后通讯时间
     */
    @Excel(name = "最后通讯时间")
    private String lastGatherTime;

    public static DeviceExportDto convert(Device device){
        DeviceExportDto res = new DeviceExportDto();
        res.setDeviceCode(device.getDeviceCode());
        res.setCategoryId(device.getCategoryId());
        res.setSpaceId(device.getSpaceId());
        res.setDeviceName(device.getDeviceName());
        res.setRunState(device.getRunState());
        res.setLastGatherTime(device.getLastGatherTime() != null ? device.getLastGatherTime().format(formatter) : null);
        res.setRemark(device.getRemark());
        return res;
    }

}
