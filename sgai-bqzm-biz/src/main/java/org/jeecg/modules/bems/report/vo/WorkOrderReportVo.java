package org.jeecg.modules.bems.report.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 工单报表
 */
@Data
@ApiModel(value = "工单报表", description = "工单处理效率、满意度、成本")
public class WorkOrderReportVo {

    @ApiModelProperty(value = "汇总数据")
    private Map<String, Object> summary;

    @ApiModelProperty(value = "工单状态统计")
    private List<StatusStatisticsItem> statusStatistics;

    @ApiModelProperty(value = "工单来源统计")
    private List<SourceStatisticsItem> sourceStatistics;

    @ApiModelProperty(value = "优先级统计")
    private List<PriorityStatisticsItem> priorityStatistics;

    @ApiModelProperty(value = "处理效率统计")
    private List<ProcessEfficiencyItem> processEfficiency;

    @ApiModelProperty(value = "工单趋势（按天）")
    private List<WorkOrderTrendItem> trend;

    @Data
    @ApiModel("工单状态统计")
    public static class StatusStatisticsItem {
        @ApiModelProperty("状态")
        private String status;
        @ApiModelProperty("数量")
        private Long count;
        @ApiModelProperty("占比")
        private BigDecimal ratio;
    }

    @Data
    @ApiModel("工单来源统计")
    public static class SourceStatisticsItem {
        @ApiModelProperty("来源")
        private String source;
        @ApiModelProperty("数量")
        private Long count;
    }

    @Data
    @ApiModel("优先级统计")
    public static class PriorityStatisticsItem {
        @ApiModelProperty("优先级")
        private String priority;
        @ApiModelProperty("数量")
        private Long count;
        @ApiModelProperty("已完成")
        private Long completedCount;
    }

    @Data
    @ApiModel("处理效率")
    public static class ProcessEfficiencyItem {
        @ApiModelProperty("负责人/区域")
        private String name;
        @ApiModelProperty("工单总数")
        private Long total;
        @ApiModelProperty("已完成")
        private Long completed;
        @ApiModelProperty("处理中")
        private Long processing;
        @ApiModelProperty("待处理")
        private Long pending;
        @ApiModelProperty("完成率(%)")
        private BigDecimal completionRate;
        @ApiModelProperty("平均处理时长(小时)")
        private BigDecimal avgHandleHours;
    }

    @Data
    @ApiModel("工单趋势")
    public static class WorkOrderTrendItem {
        @ApiModelProperty("日期")
        private String date;
        @ApiModelProperty("新增工单")
        private Long created;
        @ApiModelProperty("已完成")
        private Long completed;
    }
}
