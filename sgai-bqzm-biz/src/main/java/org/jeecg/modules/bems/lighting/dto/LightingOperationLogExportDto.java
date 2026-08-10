package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

/**
 * 控制日志 Excel 导出 DTO（查询信息借鉴 bems/lighting/operationLog/listPage）
 */
@Data
public class LightingOperationLogExportDto {

    @Excel(name = "日志类型", width = 12, orderNum = "1")
    private String logType;

    @Excel(name = "关联类型", width = 10, orderNum = "2")
    private String relType;

    @Excel(name = "关联ID", width = 22, orderNum = "3")
    private String relId;

    @Excel(name = "名称", width = 25, orderNum = "4")
    private String name;

    @Excel(name = "操作类型", width = 10, orderNum = "5")
    private String operationType;

    @Excel(name = "操作时间", width = 20, orderNum = "6")
    private String operationTime;

    @Excel(name = "操作人", width = 12, orderNum = "7")
    private String operationBy;

    @Excel(name = "操作人员类型", width = 14, orderNum = "8")
    private String operatorType;

    @Excel(name = "开启时间", width = 20, orderNum = "9")
    private String openTime;

    @Excel(name = "关闭时间", width = 20, orderNum = "10")
    private String closeTime;

    @Excel(name = "IP地址", width = 16, orderNum = "11")
    private String ipAddress;
}
