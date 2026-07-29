package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("lighting_plan_rel")
public class LightingPlanRel {

    /**
     * 主键
     */
    private Long id;

    /**
     * 计划id
     */
    private Long planId;

    /**
     * 关联id
     */
    private Long relId;

}
