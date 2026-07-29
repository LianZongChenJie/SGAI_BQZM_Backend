package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

/**
 * 描述:
 *
 * @author ppliu-life
 * created in 2024/4/19 16:26
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TableData extends LinkedHashMap<String, Object> {
    public void calculateSum() {
        BigDecimal sum = this.values().stream()
                .filter(item -> NumberUtils.isCreatable(item.toString()))
                .map(o -> new BigDecimal(o.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.put("sum", sum);
    }
}