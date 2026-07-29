package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

import java.util.List;

/**
 * 成本中心
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("cost_center")
public class CostCenter extends BaseEntity {
    @TableField(exist = false)
    public static final Long ROOT_ID = 0L;
    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 父级id
     */
    private Long parentId;

    /**
     * 排序字段
     */
    private Integer sort;

    @TableField(exist = false)
    private List<CostCenter> children;
}
