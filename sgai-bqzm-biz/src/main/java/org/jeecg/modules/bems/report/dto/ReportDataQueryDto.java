package org.jeecg.modules.bems.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表查询通用参数
 */
@Data
public class ReportDataQueryDto {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("区域ID列表")
    private List<Long> areaIds;

    @ApiModelProperty("设备ID列表")
    private List<Long> deviceIds;

    @ApiModelProperty("对比上期开关 y/n")
    private String comparePrevious;

    @ApiModelProperty("时间粒度：day、week、month")
    private String granularity;

    @ApiModelProperty("电价(元/kWh)")
    private java.math.BigDecimal price;
}
