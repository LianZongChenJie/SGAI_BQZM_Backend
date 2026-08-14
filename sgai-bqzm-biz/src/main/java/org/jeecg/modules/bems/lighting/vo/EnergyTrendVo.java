package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 逐时趋势（Top5 对比 / 全园）
 */
@Data
@ApiModel(value = "逐时能耗趋势", description = "每小时用电量趋势（kW）")
public class EnergyTrendVo {

    @ApiModelProperty(value = "小时轴，24 个点（00:00 ~ 23:00）")
    private List<String> hours;

    @ApiModelProperty(value = "序列（按地块时为 Top5，其他级别为全园）")
    private List<Series> series;

    @Data
    @ApiModel(value = "趋势序列")
    public static class Series {

        @ApiModelProperty(value = "序列名称")
        private String name;

        @ApiModelProperty(value = "24 个小时点（kWh≈kW）")
        private List<BigDecimal> points;
    }
}
