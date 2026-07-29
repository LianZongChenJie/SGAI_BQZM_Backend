package org.jeecg.modules.bems.mq.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BuildingControlPointData {

    private String gatewayAdr;

    private String bacnetAdr;

    private String value;

    private String remark;

    private LocalDateTime collectionTime;
}
