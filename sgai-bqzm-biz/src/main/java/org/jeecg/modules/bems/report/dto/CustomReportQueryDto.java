package org.jeecg.modules.bems.report.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 自定义报表查询参数
 */
@Data
public class CustomReportQueryDto {

    @ApiModelProperty(value = "数据源：operation(控制日志)、fault(故障)、alarm(报警)、workOrder(工单)、energy(能耗)")
    private String dataSource;

    @ApiModelProperty(value = "维度字段列表，如 [deviceName, alarmCategoryName]")
    private List<String> dimensionFields;

    @ApiModelProperty(value = "度量字段列表，如 [count, duration]，与 measureType 对应")
    private List<String> measureFields;

    @ApiModelProperty(value = "度量类型：count、sum、avg、max、min")
    private String measureType;

    @ApiModelProperty(value = "时间范围（同 ReportDataQueryDto）")
    private ReportDataQueryDto timeRange;

    @ApiModelProperty(value = "分页页码")
    private Integer pageNo = 1;

    @ApiModelProperty(value = "分页大小")
    private Integer pageSize = 100;

    @ApiModelProperty(value = "排序字段")
    private String orderBy;

    @ApiModelProperty(value = "排序方向：asc、desc")
    private String orderDirection = "desc";
}
