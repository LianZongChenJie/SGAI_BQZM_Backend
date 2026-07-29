package org.jeecg.modules.bems.project.dto;

import lombok.Data;

@Data
public class ProjectEnergyConservationResult {

    private ProjectEnergyConservationData water;

    private ProjectEnergyConservationData electricity;
}
