package org.jeecg.modules.bems.energyAnalysis.vo.chat;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PieChatSeriesData {

    /**
     * 名称
     */
    private String name;

    /**
     * 父级名称
     */
    private String parentName;

    /**
     * 数值
     */
    private BigDecimal value;

    /**
     * 单位
     */
    private String unit;

}
