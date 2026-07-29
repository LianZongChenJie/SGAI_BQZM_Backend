package org.jeecg.modules.bems.mq.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class EnergyConsumptionChangeMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long deviceId;

    private String hour;
}
