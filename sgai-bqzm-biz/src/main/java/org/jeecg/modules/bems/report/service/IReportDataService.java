package org.jeecg.modules.bems.report.service;

import org.jeecg.modules.bems.report.dto.CustomReportQueryDto;
import org.jeecg.modules.bems.report.dto.ReportDataQueryDto;
import org.jeecg.modules.bems.report.vo.*;

public interface IReportDataService {

    /**
     * 运行报表
     * @param params 查询参数
     * @return 运行报表数据
     */
    OperationReportVo operationReport(ReportDataQueryDto params);

    /**
     * 能耗报表
     * @param params 查询参数
     * @return 能耗报表数据
     */
    EnergyReportVo energyReport(ReportDataQueryDto params);

    /**
     * 故障报表
     * @param params 查询参数
     * @return 故障报表数据
     */
    FaultReportVo faultReport(ReportDataQueryDto params);

    /**
     * 报警报表
     * @param params 查询参数
     * @return 报警报表数据
     */
    AlarmReportVo alarmReport(ReportDataQueryDto params);

    /**
     * 工单报表
     * @param params 查询参数
     * @return 工单报表数据
     */
    WorkOrderReportVo workOrderReport(ReportDataQueryDto params);

    /**
     * 自定义报表
     * @param params 查询参数
     * @return 自定义报表数据
     */
    CustomReportVo customReport(CustomReportQueryDto params);
}
