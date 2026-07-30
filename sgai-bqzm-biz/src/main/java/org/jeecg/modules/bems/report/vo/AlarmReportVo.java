package org.jeecg.modules.bems.report.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 报警报表
 */
@Data
@ApiModel(value = "报警报表", description = "报警分类、响应时效、处理率")
public class AlarmReportVo {

    @ApiModelProperty(value = "汇总数据")
    private Map<String, Object> summary;

    @ApiModelProperty(value = "按报警分类统计")
    private List<AlarmCategoryItem> alarmCategory;

    @ApiModelProperty(value = "按报警级别统计")
    private List<AlarmLevelItem> alarmLevel;

    @ApiModelProperty(value = "响应时效统计")
    private List<AlarmResponseTimeItem> responseTime;

    @ApiModelProperty(value = "报警趋势（按天）")
    private List<AlarmTrendItem> alarmTrend;

    @ApiModelProperty(value = "处理率")
    private AlarmHandleRate handleRate;

    @Data
    @ApiModel("报警分类")
    public static class AlarmCategoryItem {
        @ApiModelProperty("分类名称")
        private String categoryName;
        @ApiModelProperty("数量")
        private Long count;
        @ApiModelProperty("占比")
        private BigDecimal ratio;
    }

    @Data
    @ApiModel("报警级别")
    public static class AlarmLevelItem {
        @ApiModelProperty("级别名称")
        private String levelName;
        @ApiModelProperty("颜色")
        private String color;
        @ApiModelProperty("数量")
        private Long count;
        @ApiModelProperty("已处理")
        private Long handledCount;
    }

    @Data
    @ApiModel("响应时效")
    public static class AlarmResponseTimeItem {
        @ApiModelProperty("时间区间")
        private String timeRange;
        @ApiModelProperty("数量")
        private Long count;
        @ApiModelProperty("平均响应时长(分钟)")
        private BigDecimal avgResponseMinutes;
    }

    @Data
    @ApiModel("报警趋势")
    public static class AlarmTrendItem {
        @ApiModelProperty("日期")
        private String date;
        @ApiModelProperty("报警总数")
        private Long totalCount;
        @ApiModelProperty("已处理")
        private Long handledCount;
        @ApiModelProperty("未处理")
        private Long unhandledCount;
    }

    @Data
    @ApiModel("处理率")
    public static class AlarmHandleRate {
        @ApiModelProperty("总报警数")
        private Long total;
        @ApiModelProperty("已处理")
        private Long handled;
        @ApiModelProperty("未处理")
        private Long unhandled;
        @ApiModelProperty("处理率(%)")
        private BigDecimal handleRate;
        @ApiModelProperty("平均响应时长(分钟)")
        private BigDecimal avgResponseMinutes;
    }
}
