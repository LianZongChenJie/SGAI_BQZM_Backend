package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 今日能耗总览（能耗排名 Top15 + 能耗占比 Top5/其他）
 * 供能耗统计页一次返回排名与占比两块数据
 */
@Data
@ApiModel(value = "今日能耗总览", description = "能耗排名(Top15) + 能耗占比(Top5+其他) + 逐时趋势(Top5)")
public class EnergyOverviewVo {

    @ApiModelProperty(value = "能耗排名（Top15 箱子今日累计用电量，降序）")
    private List<EnergyRankItemVo> ranking;

    @ApiModelProperty(value = "能耗占比（Top5 + 其他）")
    private List<EnergyProportionVo> proportion;

    @ApiModelProperty(value = "逐时趋势（Top5 箱子今日逐时用电量对比）")
    private EnergyTrendVo hourlyTrend;
}
