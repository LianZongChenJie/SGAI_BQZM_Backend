package org.jeecg.modules.bems.energyAnalysis.dto;

import lombok.Data;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointVo;

import java.util.List;

@Data
public class CostCenterRelDto {

    private Long costCenterId;
    private List<MeteringPointVo> relList;
}
