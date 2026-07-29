package org.jeecg.modules.bems.dataBoard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

@Data
public class StatisticsVo {

    /**
     * 名称
     */
    private String name;

    /**
     * 值
     */
    private String value;

    /**
     * 单位
     */
    private String unit;

    /**
     * 同比
     */
    private String yoy;

    /**
     * 环比
     */
    private String mom;

    /**
     * 计算环比增长率
     *
     * @param before 上期数值
     * @param after  本期数值
     * @return 环比增长率（以百分比形式表示，保留两位小数）
     */
    public String rate(BigDecimal before, BigDecimal after) {
        if (before == null || after == null || before.compareTo(BigDecimal.ZERO) == 0) {
            // 如果分母为零或者任一数据为空，则返回特定信息
            return "";
        } else {
            // 计算环比增长率
            BigDecimal growthRate = after.subtract(before).divide(before, 4, RoundingMode.HALF_UP);
            // 格式化结果为百分比形式，保留两位小数
            DecimalFormat df = new DecimalFormat("+0.00%;-0.00%");
            return df.format(growthRate);
        }
    }
}
