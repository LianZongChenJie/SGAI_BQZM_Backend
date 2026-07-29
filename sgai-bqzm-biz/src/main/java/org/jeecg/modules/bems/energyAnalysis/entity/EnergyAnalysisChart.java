package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

/**
 * 能效分析配置-图标
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("energy_analysis_chart")
public class EnergyAnalysisChart extends BaseEntity {

    /**
     * 能效分析配置id
     */
    private Long configId;

    /**
     * 图标名称
     */
    private String chartName;

    /**
     * 图表类型。饼：pie；柱状：bar；折线：line；堆叠柱状：stackedColumn；
     */
    private String chartType;

    /**
     * 计量规则id
     */
    private Long pointId;
    /**
     * 排序字段
     */
    private Long sort;

    /**
     * 单位
     */
    private String unit;
}
