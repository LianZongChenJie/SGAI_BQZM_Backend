package org.jeecg.modules.bems.report.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 故障报表
 */
@Data
@ApiModel(value = "故障报表", description = "故障统计、时长、故障类型")
public class FaultReportVo {

    @ApiModelProperty(value = "汇总数据")
    private Map<String, Object> summary;

    @ApiModelProperty(value = "故障统计（按区域/设备）")
    private List<FaultStatisticsItem> faultStatistics;

    @ApiModelProperty(value = "故障类型分布")
    private List<FaultTypeItem> faultTypeDistribution;

    @ApiModelProperty(value = "故障等级分布")
    private List<FaultLevelItem> faultLevelDistribution;

    @ApiModelProperty(value = "故障趋势（按天）")
    private List<FaultTrendItem> faultTrend;

    @ApiModelProperty(value = "故障TOP榜")
    private List<FaultTopItem> faultTop;

    @Data
    @ApiModel("故障统计")
    public static class FaultStatisticsItem {
        @ApiModelProperty("区域/设备ID")
        private Long id;
        @ApiModelProperty("名称")
        private String name;
        @ApiModelProperty("故障次数")
        private Long faultCount;
        @ApiModelProperty("已恢复次数")
        private Long recoveredCount;
        @ApiModelProperty("未处理次数")
        private Long unhandledCount;
        @ApiModelProperty("总故障时长(分钟)")
        private Long totalDuration;
        @ApiModelProperty("平均故障时长(分钟)")
        private Long avgDuration;
    }

    @Data
    @ApiModel("故障类型分布")
    public static class FaultTypeItem {
        @ApiModelProperty("故障类型")
        private String faultType;
        @ApiModelProperty("次数")
        private Long count;
        @ApiModelProperty("占比")
        private BigDecimal ratio;
    }

    @Data
    @ApiModel("故障等级分布")
    public static class FaultLevelItem {
        @ApiModelProperty("故障等级")
        private String faultLevel;
        @ApiModelProperty("次数")
        private Long count;
    }

    @Data
    @ApiModel("故障趋势")
    public static class FaultTrendItem {
        @ApiModelProperty("日期")
        private String date;
        @ApiModelProperty("故障次数")
        private Long count;
        @ApiModelProperty("已恢复")
        private Long recoveredCount;
    }

    @Data
    @ApiModel("故障TOP榜")
    public static class FaultTopItem {
        @ApiModelProperty("设备ID")
        private Long deviceId;
        @ApiModelProperty("设备名称")
        private String deviceName;
        @ApiModelProperty("故障次数")
        private Long faultCount;
        @ApiModelProperty("故障时长(分钟)")
        private Long totalDuration;
    }
}
