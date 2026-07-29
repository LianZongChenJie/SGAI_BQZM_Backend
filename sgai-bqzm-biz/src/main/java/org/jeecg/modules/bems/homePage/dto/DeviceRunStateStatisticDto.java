package org.jeecg.modules.bems.homePage.dto;

import lombok.Data;

@Data
public class DeviceRunStateStatisticDto {

    /**
     * 设备类别名称
     */
    private String categoryName;
    /**
     * 离线数量
     */
    private Long offLineNum;
    /**
     * 在线数量
     */
    private Long onLineNum;
    /**
     * 总数
     */
    private Long totalNum;

}
