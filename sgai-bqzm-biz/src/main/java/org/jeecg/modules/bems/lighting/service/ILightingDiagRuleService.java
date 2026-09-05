package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingDiagRule;

import java.util.List;

public interface ILightingDiagRuleService extends IService<LightingDiagRule> {

    /**
     * 查询启用规则列表（可按类型分类过滤，如 数据/供电/线路/负载/控制）
     */
    List<LightingDiagRule> listEnabled(String domain);

    /**
     * 按规则编码查询启用规则
     */
    LightingDiagRule getEnabledByCode(String ruleCode);
}
