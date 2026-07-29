package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 首页-地块统计VO
 */
@Data
@ApiModel(value = "地块统计", description = "地块数量和覆盖度")
public class AreaStatisticsVo {

    @ApiModelProperty(value = "地块总数")
    private Long totalCount;

    @ApiModelProperty(value = "覆盖度（百分比）")
    private BigDecimal coverageRate;

    @ApiModelProperty(value = "已覆盖地块数")
    private Long coveredCount;
}
