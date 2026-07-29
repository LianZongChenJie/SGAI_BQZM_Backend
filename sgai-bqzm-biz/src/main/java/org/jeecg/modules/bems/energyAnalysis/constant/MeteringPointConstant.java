package org.jeecg.modules.bems.energyAnalysis.constant;

import java.util.ArrayList;
import java.util.List;

public class MeteringPointConstant {

    /**
     * 运行拓扑
     */
    public static final String TOPOLOGY_RUN = "run";

    /**
     * 空间拓扑
     */
    public static final String TOPOLOGY_SPACE = "space";

    /**
     * 专业拓扑
     */
    public static final String TOPOLOGY_SPECIALTY = "specialty";

    /**
     * 电专业计量总点位
     */
    public static final Long ELECTRICITY_SPECIALTY_POINT_ID = 5L;
    /**
     * 电专业计量分项点位
     */
    public static final List<Long> ELECTRICITY_SPECIALTY_POINT_IDS = new ArrayList<Long>(){{
        add(6L);
        add(7L);
        add(8L);
        add(9L);
    }};

    /**
     * 计量规则分类
     */
    public static final String DICT_ENERGY_FLOW_TYPE = "energy_flow_type";
}
