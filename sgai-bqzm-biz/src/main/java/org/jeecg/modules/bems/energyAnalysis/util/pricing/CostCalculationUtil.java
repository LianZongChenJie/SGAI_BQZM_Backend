package org.jeecg.modules.bems.energyAnalysis.util.pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用能成本计算
 */
public class CostCalculationUtil {
    /**
     * 获取当前能耗成本-阶梯计价
     * @param pricings 阶梯价格
     * @param history 历史用量
     * @param now 当前用量
     */
    public static BigDecimal calculationLadderPricing(List<LadderPricing> pricings, BigDecimal history, BigDecimal now) {
        BigDecimal total = history.add(now);
        BigDecimal cost = BigDecimal.ZERO;
        for (LadderPricing pricing : pricings) {
            if (total.compareTo(pricing.getStepMin()) <= 0) {
                continue;
            }
            if (pricing.getStepMax() == null) {
                if (history.compareTo(pricing.getStepMin()) >= 0) {
                    cost = cost.add(now.multiply(pricing.getPricing()));
                } else {
                    cost = cost.add(total.subtract(pricing.getStepMin()).multiply(pricing.getPricing()));
                }
            } else if (total.compareTo(pricing.getStepMax()) <= 0) {
                if (history.compareTo(pricing.getStepMin()) >= 0) {
                    cost = cost.add(now.multiply(pricing.getPricing()));
                } else {
                    cost = cost.add(total.subtract(pricing.getStepMin()).multiply(pricing.getPricing()));
                }
            } else {
                if (history.compareTo(pricing.getStepMax()) >= 0) {
                    continue;
                } else if (history.compareTo(pricing.getStepMin()) >= 0) {
                    cost = cost.add(pricing.getStepMax().subtract(history).multiply(pricing.getPricing()));
                } else {
                    cost = cost.add(pricing.getStepMax().subtract(pricing.getStepMin()).multiply(pricing.getPricing()));
                }
            }
        }
        return cost;
    }
}
