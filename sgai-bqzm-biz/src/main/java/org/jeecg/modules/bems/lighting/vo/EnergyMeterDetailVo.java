package org.jeecg.modules.bems.lighting.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 箱子表底/累计用电量明细（区间抄表-详情）
 * 用于"能耗-区间查询"每行"详情"：按网关 + 时间区间返回区间内逐条表底记录，
 * 后端已算好每段用电量增量（本条表底 - 上一条表底）与从区间基准起的累计用电量。
 */
@Data
@ApiModel(value = "箱子表底/累计用电量明细", description = "区间抄表详情：区间内逐条表底与用电量")
public class EnergyMeterDetailVo {

    @ApiModelProperty(value = "网关编号（箱子唯一标识）")
    private String gatewayCode;

    @ApiModelProperty(value = "采集时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date collectTime;

    @ApiModelProperty(value = "表底（kWh）")
    private BigDecimal totalEnergy;

    @ApiModelProperty(value = "本段用电量（kWh）= 本条表底 - 上一条表底；首条相对区间开始前最近一次基准表底计算")
    private BigDecimal energy;

    @ApiModelProperty(value = "累计用电量（kWh）自区间开始基准起的累计")
    private BigDecimal cumulative;
}
