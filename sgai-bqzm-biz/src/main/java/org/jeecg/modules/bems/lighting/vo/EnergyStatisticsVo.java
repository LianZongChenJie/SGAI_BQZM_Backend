package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 首页-用电统计VO
 */
@Data
@ApiModel(value = "用电统计", description = "今日用电和较昨日对比")
public class EnergyStatisticsVo {

    @ApiModelProperty(value = "今日用电量（kWh）")
    private BigDecimal todayEnergy;

    @ApiModelProperty(value = "昨日用电量（kWh）")
    private BigDecimal yesterdayEnergy;

    @ApiModelProperty(value = "环比变化（百分比，正数为增长，负数为下降）")
    private BigDecimal changeRate;

    @ApiModelProperty(value = "变化趋势：up-上升、down-下降、equal-持平")
    private String trend;
}
