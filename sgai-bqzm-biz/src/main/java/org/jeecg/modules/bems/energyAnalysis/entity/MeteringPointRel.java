package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 计量点关联设备点位
 */
@Data
@TableName("metering_point_rel")
@AllArgsConstructor
@NoArgsConstructor
public class MeteringPointRel {

    /**
     * 关联类型：设备
     */
    public static final String TYPE_DEVICE = "1";
    /**
     * 关联类型：计量点
     */
    public static final String TYPE_METERING_POINT = "2";

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 计量点id
     */
    private Long meteringPointId;

    /**
     * 关联id
     */
    private Long relId;

    /**
     * 关联类型。设备：1；计量点：2
     */
    private String relType;
}
