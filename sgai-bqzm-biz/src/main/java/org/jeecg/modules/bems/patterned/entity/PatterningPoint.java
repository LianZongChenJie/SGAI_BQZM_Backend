package org.jeecg.modules.bems.patterned.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("patterning_point")
@Data
public class PatterningPoint{

    /**主键*/
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 模式化策略主键. */
    private Long patternStrategyId;
    /** 设备主键. */
    private Long deviceId;
    /** 设备编码. */
    private String deviceCode;
    /** 设备名称. */
    private String deviceName;
    /** 空间主键. */
    private Long spaceId;
    /** 空间名称. */
    private String spaceName;
    /** 条件值. */
    private String conditionValue;
    /** 点位名称 */
    private String pointName;
    /** 点位ID*/
    private Long pointId;
}
