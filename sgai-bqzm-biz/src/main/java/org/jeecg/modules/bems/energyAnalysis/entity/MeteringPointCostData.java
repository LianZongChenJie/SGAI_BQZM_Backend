package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public abstract class MeteringPointCostData {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long meteringPointId;

    private LocalDateTime time;

    private BigDecimal value;

    private BigDecimal cost;
}
