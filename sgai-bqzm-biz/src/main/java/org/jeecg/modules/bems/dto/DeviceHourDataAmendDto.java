package org.jeecg.modules.bems.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeviceHourDataAmendDto {

    /**
     * 小时能耗id
     */
    private Long id;

    /**
     * 修正后最终值
     */
    private BigDecimal value;

}
