package org.jeecg.modules.bems.patterned.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("linkage_strategy")
public class LinkageStrategy extends BaseEntity {

    /** 启用状态. */
    public static final String Enable = "1";
    /** 禁用状态. */
    public static final String Disable = "0";

    /** 策略编码 */
    private String strategyCode;
    /** 策略名称. */
    private String strategyName;
    /** 策略目标. */
    private String strategyTarget;
    /** 前置设备. */
    private String frontDevice;
    /** 后置设备. */
    private String rearDevice;
    /** 启动状态【0禁用 ，1启用】 */
    private String enabledStatus;

    /** 设备前置对象集合. */
    @TableField(exist = false)
    private List<FrontPoint> frontPointList;
    /** 设备后置对象集合.*/
    @TableField(exist = false)
    private List<RearPoint> rearPointList;
}
