package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

/**
 * 区域列表 Excel 导出 DTO（查询信息借鉴 bems/lighting/area/listPage1）
 */
@Data
public class LightingAreaExportDto {

    @Excel(name = "区域名称", width = 25, orderNum = "1")
    private String areaName;

    @Excel(name = "区域编码", width = 20, orderNum = "2")
    private String areaCode;

    @Excel(name = "设备编号", width = 15, orderNum = "3")
    private String deviceNo;

    @Excel(name = "空间名称", width = 15, orderNum = "3")
    private String spaceName;

    @Excel(name = "片区名称", width = 15, orderNum = "4")
    private String districtName;

    @Excel(name = "关联名称", width = 20, orderNum = "5")
    private String relName;

    @Excel(name = "状态", width = 10, orderNum = "6")
    private String status;

    @Excel(name = "类型", width = 10, orderNum = "7")
    private String type;

    @Excel(name = "回路数", width = 10, orderNum = "8")
    private Integer circuitCount;

    @Excel(name = "在线数", width = 10, orderNum = "9")
    private Integer onlineCount;

    @Excel(name = "今日用电量(kWh)", width = 16, orderNum = "10")
    private Double todayEnergy;

    @Excel(name = "累计用电量(kWh)", width = 16, orderNum = "11")
    private Double totalEnergy;

    @Excel(name = "开灯时间", width = 20, orderNum = "12")
    private String startTime;

    @Excel(name = "关灯时间", width = 20, orderNum = "13")
    private String closingTime;

    @Excel(name = "运行时长(秒)", width = 14, orderNum = "14")
    private Long allDuration;

    @Excel(name = "排序", width = 8, orderNum = "15")
    private Long sort;

    @Excel(name = "备注", width = 20, orderNum = "16")
    private String remark;
}
