package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnergyFlowDiagramVo{

    private Long id;

    private Long parentId;

    private String parentNodeName;

    private String type;

    private String nodeName;

    // 使用量
    private BigDecimal value;

    /**
     * 单位。例：kW、kWh,t
     */
    private String unit;

    /**
     * 单位名称。例：千瓦、千瓦时、吨
     */
    private String unitName;

    /**
     * 显示类型。节点：0；数据：1
     */
    private String showType;
}
