package org.jeecg.modules.bems.report.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.report.dto.CustomReportQueryDto;
import org.jeecg.modules.bems.report.dto.ReportDataQueryDto;
import org.jeecg.modules.bems.report.service.IReportDataService;
import org.jeecg.modules.bems.report.vo.*;
import org.springframework.web.bind.annotation.*;

/**
 * 报表中心 - 6大报表数据接口
 *
 * 运行报表 / 能耗报表 / 故障报表 / 报警报表 / 工单报表 / 自定义报表
 */
@Api(tags = "报表中心-数据查询")
@RestController
@RequestMapping("/bems/report/data")
@AllArgsConstructor
public class ReportDataController {

    private final IReportDataService reportDataService;

    // ==================== 1. 运行报表 ====================

    @ApiOperation("运行报表 - 设备运行展示、开关记录、模式统计")
    @GetMapping("/operation")
    public Result<OperationReportVo> operationReport(ReportDataQueryDto params) {
        return Result.ok(reportDataService.operationReport(params));
    }

    // ==================== 2. 能耗报表 ====================

    @ApiOperation("能耗报表 - 用电量、电费、能耗对比")
    @GetMapping("/energy")
    public Result<EnergyReportVo> energyReport(ReportDataQueryDto params) {
        return Result.ok(reportDataService.energyReport(params));
    }

    // ==================== 3. 故障报表 ====================

    @ApiOperation("故障报表 - 故障统计、时长、故障类型")
    @GetMapping("/fault")
    public Result<FaultReportVo> faultReport(ReportDataQueryDto params) {
        return Result.ok(reportDataService.faultReport(params));
    }

    // ==================== 4. 报警报表 ====================

    @ApiOperation("报警报表 - 报警分类、响应时效、处理率")
    @GetMapping("/alarm")
    public Result<AlarmReportVo> alarmReport(ReportDataQueryDto params) {
        return Result.ok(reportDataService.alarmReport(params));
    }

    // ==================== 5. 工单报表 ====================

    @ApiOperation("工单报表 - 工单处理效率、状态、优先级")
    @GetMapping("/workOrder")
    public Result<WorkOrderReportVo> workOrderReport(ReportDataQueryDto params) {
        return Result.ok(reportDataService.workOrderReport(params));
    }

    // ==================== 6. 自定义报表 ====================

    @ApiOperation("自定义报表 - 自定义维度、字段、时间范围")
    @PostMapping("/custom")
    public Result<CustomReportVo> customReport(@RequestBody CustomReportQueryDto params) {
        return Result.ok(reportDataService.customReport(params));
    }

    @ApiOperation("自定义报表 - GET方式（简化调用）")
    @GetMapping("/custom")
    public Result<CustomReportVo> customReportGet(CustomReportQueryDto params) {
        return Result.ok(reportDataService.customReport(params));
    }
}
