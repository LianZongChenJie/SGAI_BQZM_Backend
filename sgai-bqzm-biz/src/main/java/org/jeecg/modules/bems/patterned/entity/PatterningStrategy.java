package org.jeecg.modules.bems.patterned.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

import java.util.List;

/**
 * 场景控制
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("patterning_strategy")
public class PatterningStrategy extends BaseEntity {

    /** 启用状态. */
    public static final String Enable = "1";
    /** 禁用状态. */
    public static final String Disable = "0";

    /** 自动模式. */
    public static final String ModelType_Auto = "自动";
    /** 手动模式. */
    public static final String ModelType_Manual = "手动";


    /** 策略编号. */
    private String strategyCode;
    /** 策略名称 */
    private String strategyName;
    /** 应用场景 */
    private String strategyScene;
    /** 策略目的 */
    private String strategyTarget;
    /** 启动状态【0禁用 ，1启用】 */
    private String enabledStatus;
    /** 是否为复合专业. */
    private String compositeSpecialtyFlag;
    /** 空间主键. */
    private Long spaceId;
    /** 空间名称. */
    private String spaceName;
    /** 分组名称. */
    private String groupName;
    /** 分组主键. */
    private Long groupId;
    /** 模式类型【手动/自动】. */
    private String modelType;
    /** 专业ID. */
    private Long professionalId;
    /** 专业名称. */
    private String professionalName;
    /** 前后关联中间表集合 */
    @TableField(exist = false)
    private List<PatterningRelated> patterningRelatedList;
    /** 设备点位列表 */
    @TableField(exist = false)
    private List<PatterningPoint> patterningPointList;
    /** 执行设备/参数，描述 */
    private String executeDevice;
}
