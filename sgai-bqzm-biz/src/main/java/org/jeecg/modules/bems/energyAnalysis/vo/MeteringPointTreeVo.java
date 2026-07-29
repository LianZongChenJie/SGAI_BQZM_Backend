package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeteringPointTreeVo {

    private Long id;

    private String type;

    private String nodeCode;

    private String nodeName;

    private Long categoryId;

    private Long spaceId;

    private Long meteringUnit;

    private Long parentId;

    private Integer sort;

    private String formula;

    private List<MeteringPointTreeVo> children;

    public static MeteringPointTreeVo convert(MeteringPoint data){
        return new MeteringPointTreeVo(data.getId(), data.getType(),data.getNodeCode(), data.getNodeName(),data.getCategoryId(),data.getSpaceId(),data.getMeteringUnit(), data.getParentId(), data.getSort(),data.getFormula(),null);
    }

}
