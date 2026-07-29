package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

/**
 * 能效分析配置
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("energy_analysis_config")
public class EnergyAnalysisConfig extends BaseEntity {

    /**
     * 启用
     */
    public static final String STATUS_ENABLE = "1";
    /**
     * 禁用
     */
    public static final String STATUS_DISABLE = "0";


    /**
     * 名称
     */
    private String name;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态。启用：1；禁用：0
     */
    private String status;

}
