package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 首页-在线设备统计VO
 */
@Data
@ApiModel(value = "在线设备统计", description = "在线设备和在线率")
public class OnlineStatisticsVo {

    @ApiModelProperty(value = "设备总数")
    private Long totalCount;

    @ApiModelProperty(value = "在线设备数")
    private Long onlineCount;

    @ApiModelProperty(value = "离线设备数")
    private Long offlineCount;

    @ApiModelProperty(value = "在线率（百分比）")
    private BigDecimal onlineRate;
}
