package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.Data;
import org.jeecg.modules.bems.energyAnalysis.entity.CostCenterRel;

@Data
public class MeteringPointVo {
    private Long pointId;

    private String pointCode;

    private String pointName;

    private Long categoryId;

    private Long spaceId;

    /**
     * 类型。点位：1；计量设备：2
     */
    private String pointType;

    public static MeteringPointVo convert(CostCenterRel rel){
        MeteringPointVo meteringPointVo = new MeteringPointVo();
        meteringPointVo.setPointId(rel.getRelId());
        meteringPointVo.setPointName(rel.getPointName());
        meteringPointVo.setPointCode(rel.getPointCode());
        meteringPointVo.setCategoryId(rel.getCategoryId());
        meteringPointVo.setSpaceId(rel.getSpaceId());
        meteringPointVo.setPointType(rel.getRelType());
        return meteringPointVo;
    }
}
