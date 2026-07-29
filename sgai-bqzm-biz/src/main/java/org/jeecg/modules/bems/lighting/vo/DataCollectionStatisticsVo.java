package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 数据汇集统计VO
 */
@Data
@ApiModel(value = "数据汇集统计", description = "数据汇集统计-折线图")
public class DataCollectionStatisticsVo {

    @ApiModelProperty(value = "时间点列表")
    private List<String> times;

    @ApiModelProperty(value = "实时数据量")
    private List<Long> realtimeData;

    @ApiModelProperty(value = "历史数据量")
    private List<Long> historyData;
}
