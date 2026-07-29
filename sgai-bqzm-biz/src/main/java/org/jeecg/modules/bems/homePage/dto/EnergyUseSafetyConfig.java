package org.jeecg.modules.bems.homePage.dto;

import lombok.Data;

import java.util.List;

/**
 * 北艺-看板-用能安全数据配置
 */
@Data
public class EnergyUseSafetyConfig {

    /**
     * 名称
     */
    private String name;
    /**
     * 设备ids
     */
    private List<Long> deviceIds;

    /**
     * 额定负荷属性编码
     */
    private String ratedLoadAttributeCode;

    /**
     * 当前负荷属性编码
     */
    private String currentLoadAttributeCode;
}
