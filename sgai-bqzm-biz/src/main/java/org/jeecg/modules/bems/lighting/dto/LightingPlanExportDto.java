package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

/**
 * 计划/场景列表 Excel 导出 DTO
 * 查询信息借鉴 bems/lighting/plan/listPage（场景 listPage 出参同为 LightingPlan 结构，共用此 DTO）
 */
@Data
public class LightingPlanExportDto {

    @Excel(name = "名称", width = 25, orderNum = "1")
    private String planName;

    @Excel(name = "关联类型", width = 10, orderNum = "2")
    private String relType;

    @Excel(name = "关联ID", width = 25, orderNum = "3")
    private String relIds;

    @Excel(name = "执行时间", width = 12, orderNum = "4")
    private String executionTime;

    @Excel(name = "操作类型", width = 10, orderNum = "5")
    private String operationType;

    @Excel(name = "状态", width = 10, orderNum = "6")
    private String status;

    @Excel(name = "计划类型", width = 12, orderNum = "7")
    private String planType;

    @Excel(name = "类别", width = 12, orderNum = "8")
    private String category;

    @Excel(name = "周期类型", width = 12, orderNum = "9")
    private String cycleType;

    @Excel(name = "开始日期", width = 14, orderNum = "10")
    private String startDate;

    @Excel(name = "结束日期", width = 14, orderNum = "11")
    private String endDate;

    @Excel(name = "启用星期", width = 14, orderNum = "12")
    private String enabledWeek;

    @Excel(name = "标签ID", width = 14, orderNum = "13")
    private String tagId;

    @Excel(name = "标签名称", width = 16, orderNum = "14")
    private String tagName;

    @Excel(name = "泛光节目ID", width = 16, orderNum = "15")
    private String groupId;

    @Excel(name = "排序", width = 8, orderNum = "16")
    private Long sort;

    @Excel(name = "备注", width = 20, orderNum = "17")
    private String remark;

    @Excel(name = "创建人", width = 12, orderNum = "18")
    private String createBy;

    @Excel(name = "创建时间", width = 20, orderNum = "19")
    private String createTime;

    @Excel(name = "更新人", width = 12, orderNum = "20")
    private String updateBy;

    @Excel(name = "更新时间", width = 20, orderNum = "21")
    private String updateTime;

    @Excel(name = "节目场景ID", width = 25, orderNum = "22")
    private String programSceneIds;
}
