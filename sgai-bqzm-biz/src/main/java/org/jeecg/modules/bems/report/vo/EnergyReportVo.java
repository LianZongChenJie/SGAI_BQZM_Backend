package org.jeecg.modules.bems.report.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 能耗报表
 */
@Data
@ApiModel(value = "能耗报表", description = "用电量、电费、采暖、能耗对比")
public class EnergyReportVo {

    @ApiModelProperty(value = "汇总数据")
    private Map<String, Object> summary;

    @ApiModelProperty(value = "用电量统计（按区域/时间）")
    private List<EnergyStatisticsItem> energyStatistics;

    @ApiModelProperty(value = "电费统计")
    private List<EnergyCostItem> energyCost;

    @ApiModelProperty(value = "能耗对比（同期/环比）")
    private List<EnergyCompareItem> energyCompare;

    @ApiModelProperty(value = "能耗类型分布")
    private List<EnergyTypeDistributionItem> energyTypeDistribution;

    @Data
    @ApiModel("用电量统计")
    public static class EnergyStatisticsItem {
        @ApiModelProperty("时间标签/区域ID")
        private String label;
        @ApiModelProperty("区域/回路名称")
        private String name;
        @ApiModelProperty("用电量(kWh)")
        private BigDecimal energyValue;
        @ApiModelProperty("时间")
        private String time;
        @ApiModelProperty("类型")
        private String type;
    }

    @Data
    @ApiModel("电费统计")
    public static class EnergyCostItem {
        @ApiModelProperty("区域/回路")
        private String name;
        @ApiModelProperty("用电量(kWh)")
        private BigDecimal energyValue;
        @ApiModelProperty("电费(元)")
        private BigDecimal cost;
        @ApiModelProperty("电价(元/kWh)")
        private BigDecimal price;
    }

    @Data
    @ApiModel("能耗对比")
    public static class EnergyCompareItem {
        @ApiModelProperty("对比项")
        private String name;
        @ApiModelProperty("本期能耗")
        private BigDecimal currentValue;
        @ApiModelProperty("上期能耗")
        private BigDecimal previousValue;
        @ApiModelProperty("环比变化")
        private BigDecimal changeRate;
    }

    @Data
    @ApiModel("能耗类型分布")
    public static class EnergyTypeDistributionItem {
        @ApiModelProperty("类型")
        private String type;
        @ApiModelProperty("能耗")
        private BigDecimal value;
        @ApiModelProperty("占比")
        private BigDecimal ratio;
    }
}
