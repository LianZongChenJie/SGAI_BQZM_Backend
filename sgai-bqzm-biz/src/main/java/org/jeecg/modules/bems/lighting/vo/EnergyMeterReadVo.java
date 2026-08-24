package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电表读数区间查询结果（按箱子/网关维度）
 * 用于"能耗汇总表-区间查询"页签：展示箱子的区域、箱子名称、开始时间表底、结束时间表底、累计用电量
 */
@Data
@ApiModel(value = "电表读数区间查询结果", description = "按区域/箱子/时间区间查询表底与累计用电量")
public class EnergyMeterReadVo {

    @ApiModelProperty(value = "地块/片区名称")
    private String districtName;

    @ApiModelProperty(value = "区域名称")
    private String areaName;

    @ApiModelProperty(value = "箱子名称（网关，如：12号网关）")
    private String boxName;

    @ApiModelProperty(value = "网关编号")
    private String gatewayCode;

    @ApiModelProperty(value = "开始时间（区间起点，取该时刻前最近一次读数）")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "开始时间表底（kWh）")
    private BigDecimal startValue;

    @ApiModelProperty(value = "结束时间（区间终点，取该时刻前最近一次读数）")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "结束时间表底（kWh）")
    private BigDecimal endValue;

    @ApiModelProperty(value = "累计用电量（kWh）= 结束表底 - 开始表底")
    private BigDecimal total;
}
