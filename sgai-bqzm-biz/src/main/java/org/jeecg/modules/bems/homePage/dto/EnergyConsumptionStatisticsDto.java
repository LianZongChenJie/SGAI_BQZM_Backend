package org.jeecg.modules.bems.homePage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnergyConsumptionStatisticsDto {

    /**
     * 能耗
     */
    private BigDecimal value;
    /**
     * 环比
     */
    private String mom;
    /**
     * 同比
     */
    private String yoy;

    /**
     * 计算能耗统计信息
     * @param value 当前值
     * @param lastValue 上期值（用于环比）
     * @param lastLastValue 去年同期值（用于同比）
     * @return 能耗统计DTO
     */
    public static EnergyConsumptionStatisticsDto calculation(BigDecimal value, BigDecimal lastValue, BigDecimal lastLastValue){
        return new EnergyConsumptionStatisticsDto(value, rate(value, lastValue), rate(value, lastLastValue));
    }

    /**
     * 计算增长率
     * @param current 当前值
     * @param previous 对比值
     * @return 格式化的百分比字符串，如"+5.2%"或"-3.1%"
     */
    private static String rate(BigDecimal current, BigDecimal previous) {
        // 处理空值情况
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return "--";
        }

        // 计算增长率：(当前值-对比值)/对比值 * 100
        BigDecimal rate = current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // 格式化为带符号的百分比字符串
        String sign = rate.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + rate.setScale(2, RoundingMode.HALF_UP) + "%";
    }
}
