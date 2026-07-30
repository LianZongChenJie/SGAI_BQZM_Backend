package org.jeecg.modules.bems.report.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 运行报表
 */
@Data
@ApiModel(value = "运行报表", description = "设备运行展示、开关记录、模式统计")
public class OperationReportVo {

    @ApiModelProperty(value = "汇总数据")
    private Map<String, Object> summary;

    @ApiModelProperty(value = "各设备运行时长")
    private List<DeviceRunTimeItem> deviceRunTime;

    @ApiModelProperty(value = "模式统计（自动/手动）")
    private List<ModeStatisticsItem> modeStatistics;

    @ApiModelProperty(value = "控制类型统计")
    private List<TypeStatisticsItem> controlTypeStatistics;

    @ApiModelProperty(value = "开关记录明细")
    private List<ControlLogItem> controlLogList;

    @Data
    @ApiModel("设备运行时长")
    public static class DeviceRunTimeItem {
        @ApiModelProperty("设备/回路ID")
        private Long id;
        @ApiModelProperty("名称")
        private String name;
        @ApiModelProperty("运行时长(小时)")
        private BigDecimal runHours;
        @ApiModelProperty("开灯次数")
        private Integer openCount;
        @ApiModelProperty("所属区域")
        private String areaName;
    }

    @Data
    @ApiModel("模式统计")
    public static class ModeStatisticsItem {
        @ApiModelProperty("模式：自动、手动")
        private String mode;
        @ApiModelProperty("次数")
        private Long count;
        @ApiModelProperty("占比")
        private BigDecimal ratio;
    }

    @Data
    @ApiModel("控制类型统计")
    public static class TypeStatisticsItem {
        @ApiModelProperty("类型：场景、回路")
        private String controlType;
        @ApiModelProperty("次数")
        private Long count;
    }

    @Data
    @ApiModel("控制日志明细")
    public static class ControlLogItem {
        @ApiModelProperty("控制时间")
        private String controlTime;
        @ApiModelProperty("控制类型")
        private String controlType;
        @ApiModelProperty("关联名称")
        private String relName;
        @ApiModelProperty("操作")
        private String operation;
        @ApiModelProperty("操作类型")
        private String operatorType;
        @ApiModelProperty("操作人")
        private String operatorName;
        @ApiModelProperty("执行结果")
        private String result;
    }
}
