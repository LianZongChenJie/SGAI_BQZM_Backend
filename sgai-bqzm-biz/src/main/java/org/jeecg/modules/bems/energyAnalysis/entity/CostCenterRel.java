package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("cost_center_rel")
public class CostCenterRel{

    public static final String REL_TYPE_METERING_POINT = "1";
    public static final String REL_TYPE_DEVICE = "2";
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 成本中心id
     */
    private Long costCenterId;

    /**
     * 关联id
     */
    private Long relId;

    private String pointName;

    private String pointCode;

    private Long categoryId;

    private Long spaceId;

    /**
     * 关联类型。计量点位：1；计量设备：2
     */
    private String relType;
}
