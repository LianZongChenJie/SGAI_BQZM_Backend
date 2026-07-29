package org.jeecg.modules.bems.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("data_minute")
public class MinuteData extends MeterData{
    private static final long serialVersionUID = 1L;
    /**
     * 起始值
     */
    private BigDecimal startValue;

    /**
     * 结束值
     */
    private BigDecimal endValue;
}
