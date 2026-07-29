package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.energyAnalysis.util.pricing.LadderPricing;
import org.jeecg.modules.bems.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 能源价格配置
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("energy_pricing_config")
public class EnergyPricingConfig extends BaseEntity {

    public static final DateTimeFormatter filedForMatter = DateTimeFormatter.ofPattern("MM-HH");


    /**
     * 电
     */
    public static final String CATEGORY_ELECTRICITY = "electricity";
    /**
     * 水
     */
    public static final String CATEGORY_WATER = "water";

    /**
     * 热
     */
    public static final String CATEGORY_HEATING = "heating";

    /**
     * 状态：启用
     */
    public static final String STATUS_ENABLE = "1";
    /**
     * 状态：禁用
     */
    public static final String STATUS_DISABLE = "0";

    /**
     * 仪表类别id
     */
    private Long categoryId;
    /**
     * 类别。电：electricity；水：water；热：heating
     */
    private String category;
    /**
     * 计价方式 1-峰谷分时计价 2-固定计价 3-阶梯计价
     */
    private String billingWay;
    /**
     * 固定单价
     */
    private BigDecimal fixedUnitPrice;
    /**
     * 阶梯计价-第一阶段-最大值
     */
    private BigDecimal step1Max;
    /**
     * 阶梯计价-第一阶段-单价
     */
    private BigDecimal step1UnitPrice;
    /**
     * 阶梯计价-第二阶段-最大值
     */
    private BigDecimal step2Max;
    /**
     * 阶梯计价-第二阶段-最小值
     */
    private BigDecimal step2Min;
    /**
     * 阶梯计价-第二阶段-单价
     */
    private BigDecimal step2UnitPrice;
    /**
     * 阶梯计价-第三阶段-最小值
     */
    private BigDecimal step3Min;
    /**
     * 阶梯计价-第三阶段-单价
     */
    private BigDecimal step3UnitPrice;
    /**
     * 峰谷分时计价-尖电价
     */
    private BigDecimal tipPrice;
    /**
     * 峰谷分时计价-峰电价
     */
    private BigDecimal peakPrice;
    /**
     * 峰谷分时计价-平电价
     */
    private BigDecimal flatPrice;
    /**
     * 峰谷分时计价-谷电价
     */
    private BigDecimal valleyPrice;
    /**
     * 峰谷分时计价-适用月份1
     */
    private String applyMonths1;
    /**
     * 峰谷分时计价-尖时段1
     */
    private String tipTimeSlot1;
    /**
     * 峰谷分时计价-峰时段1
     */
    private String peakTimeSlot1;
    /**
     * 峰谷分时计价-平时段1
     */
    private String flatTimeSlot1;
    /**
     * 峰谷分时计价-谷时段1
     */
    private String valleyTimeSlot1;
    /**
     * 峰谷分时计价-适用月份2
     */
    private String applyMonths2;
    /**
     * 峰谷分时计价-尖时段2
     */
    private String tipTimeSlot2;
    /**
     * 峰谷分时计价-峰时段2
     */
    private String peakTimeSlot2;
    /**
     * 峰谷分时计价-平时段2
     */
    private String flatTimeSlot2;
    /**
     * 峰谷分时计价-谷时段2
     */
    private String valleyTimeSlot2;

    /**
     * 启用：1；禁用：0
     */
    private String status;

    /**
     * 格式化峰谷分时计价
     * @return 格式化后的峰谷分时计价，key：MM:HH，value：价格
     */
    public Map<String,BigDecimal> formatPVTS(){
        Map<String,BigDecimal> res = new HashMap<>();
        res.putAll(formatPVTS(this.getApplyMonths1(), this.getTipTimeSlot1(), this.getPeakTimeSlot1(), this.getFlatTimeSlot1(), this.getValleyTimeSlot1(), this.getTipPrice(), this.getPeakPrice(), this.getFlatPrice(), this.getValleyPrice()));
        res.putAll(formatPVTS(this.getApplyMonths2(), this.getTipTimeSlot2(), this.getPeakTimeSlot2(), this.getFlatTimeSlot2(), this.getValleyTimeSlot2(), this.getTipPrice(), this.getPeakPrice(), this.getFlatPrice(), this.getValleyPrice()));
        return res;
    }

    public List<LadderPricing> formatLadderPricing(){
        List<LadderPricing> res = new ArrayList<>();
        res.add(new LadderPricing(BigDecimal.ZERO,this.getStep1Max(),this.getStep1UnitPrice()));
        res.add(new LadderPricing(this.getStep2Min(),this.getStep2Max(),this.getStep2UnitPrice()));
        res.add(new LadderPricing(this.getStep3Min(),null,this.getStep3UnitPrice()));
        return res;
    }

    /**
     * 获取当前能耗成本
     * @param pricings 阶梯价格
     * @param history 历史用量
     * @param now 当前用量
     */
    private BigDecimal calculationLadderPricing(List<LadderPricing> pricings, BigDecimal history, BigDecimal now) {
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

    private Map<String,BigDecimal> formatPVTS(String months, String tipTimeSlot, String peakTimeSlot, String flatTimeSlot, String valleyTimeSlot, BigDecimal tipPrice, BigDecimal peakPrice, BigDecimal flatPrice, BigDecimal valleyPrice){
        String[] applyMonths1 = months.split(",");
        Map<String,BigDecimal> res = new HashMap<>();
        for (String s : applyMonths1) {
            // 尖时段
            String[] tipTimeSlot1 = tipTimeSlot.split(",");
            for (String item : tipTimeSlot1) {
                res.put(s + "-" + item,tipPrice);
            }
            // 峰时段
            String[] peakTimeSlot1 = peakTimeSlot.split(",");
            for (String item : peakTimeSlot1) {
                res.put(s + "-" + item,peakPrice);
            }
            // 平谷时段
            String[] flatTimeSlot1 = flatTimeSlot.split(",");
            for (String item : flatTimeSlot1) {
                res.put(s + "-" + item,flatPrice);
            }
            // 谷时段
            String[] valleyTimeSlot1 = valleyTimeSlot.split(",");
            for (String item : valleyTimeSlot1) {
                res.put(s + "-" + item,valleyPrice);
            }
        }
        return res;
    }
}
