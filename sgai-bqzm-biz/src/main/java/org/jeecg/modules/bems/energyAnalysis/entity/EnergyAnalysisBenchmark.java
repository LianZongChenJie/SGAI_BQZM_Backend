package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

/**
 * 能效分析配置-基准配置
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("energy_analysis_benchmark")
public class EnergyAnalysisBenchmark extends BaseEntity {

    /**
     * 能效分析配置Id
     */
    private Long configId;
    /**
     * 文本
     */
    private String label;

    /**
     * 基准值
     */
    private String value;

    /**
     * 运算符
     */
    private String operator;

    /**
     * 提示信息
     */
    private String content;

    /**
     * 排序字段
     */
    private Integer sort;
}
