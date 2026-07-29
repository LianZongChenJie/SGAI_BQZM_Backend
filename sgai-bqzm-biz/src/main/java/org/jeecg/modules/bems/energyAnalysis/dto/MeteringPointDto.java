package org.jeecg.modules.bems.energyAnalysis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointVo;

@EqualsAndHashCode(callSuper = true)
@Data
public class MeteringPointDto extends MeteringPointVo {

    private String type;

    private Long parentId;

    private String deviceType;

    private int pageNo = 1;

    private int pageSize = 10;
}
