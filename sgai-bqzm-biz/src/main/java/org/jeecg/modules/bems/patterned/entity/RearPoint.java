package org.jeecg.modules.bems.patterned.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 联动策略后置点位
 */
@TableName("linkage_rear_point")
@Data
public class RearPoint {
    /**主键*/
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键")
    private Long id;
    /** 设备主键. */
    private Long deviceId;
    /**
     * 设备名称
     */
    private String deviceName;
    /**
     * 空间名称
     */
    private String spaceName;
    /**联动策略主键.*/
    private Long linkageStrategyId;
    /** 条件值. */
    private String conditionValue;
    /** 点位ID*/
    private Long pointId;
    /**
     * 点位名称
     */
    private String pointName;
}
