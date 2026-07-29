package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本中心-核算成本数据
 */
@Data
public abstract class CostCenterData {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键")
    private Long id;

    /**
     * 关联类型。计量点位：1；计量设备：2
     */
    private String type;

    /**
     * 关联id
     */
    private Long relId;

    /**
     * 时间
     */
    private LocalDateTime time;

    /**
     * 用量
     */
    private BigDecimal value;

    /**
     * 成本
     */
    private BigDecimal cost;

}
