package org.jeecg.modules.bems.wd.dto;

import lombok.Data;

@Data
public class ScreenFireControlRoomDto {
    /**
     * 未持证值守人员数量
     */
    private String onDutyNoCertificate;

    /**
     * 未持证维保人员数量
     */
    private String maintenanceNoCertificate;
    /**
     * 持证维保人员数量
     */
    private String maintenanceCertificate;
    /**
     * 持证值守人员数量
     */
    private String onDutyCertificate;
    /**
     * 值守人员总数
     */
    private String onDuty;

    /**
     * 维保人员总数
     */
    private String maintenance;

}
