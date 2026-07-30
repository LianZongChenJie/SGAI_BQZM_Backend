package org.jeecg.modules.bems.report.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 自定义报表
 */
@Data
@ApiModel(value = "自定义报表", description = "自定义维度、字段、时间范围")
public class CustomReportVo {

    @ApiModelProperty(value = "动态结果集（key=行/列 name, value=数值）")
    private List<Map<String, Object>> rows;

    @ApiModelProperty(value = "列定义")
    private List<ColumnDefine> columns;

    @ApiModelProperty(value = "总记录数")
    private Long total;

    @Data
    @ApiModel("列定义")
    public static class ColumnDefine {
        @ApiModelProperty("字段名")
        private String field;
        @ApiModelProperty("显示名")
        private String label;
        @ApiModelProperty("类型：dimension（维度）、metric（度量）")
        private String type;
        @ApiModelProperty("数值类型：string、number、date")
        private String dataType;
    }
}
