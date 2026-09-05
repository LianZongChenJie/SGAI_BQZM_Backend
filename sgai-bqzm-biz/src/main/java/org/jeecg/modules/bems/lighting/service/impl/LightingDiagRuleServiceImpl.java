package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingDiagRule;
import org.jeecg.modules.bems.lighting.mapper.LightingDiagRuleMapper;
import org.jeecg.modules.bems.lighting.service.ILightingDiagRuleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LightingDiagRuleServiceImpl extends ServiceImpl<LightingDiagRuleMapper, LightingDiagRule> implements ILightingDiagRuleService {

    /** 规则状态：启用 */
    public static final String STATUS_ENABLE = "启用";

    @Override
    public List<LightingDiagRule> listEnabled(String domain) {
        return super.list(new LambdaQueryWrapper<LightingDiagRule>()
                .eq(LightingDiagRule::getStatus, STATUS_ENABLE)
                .eq(StringUtils.isNotBlank(domain), LightingDiagRule::getRuleDomain, domain)
                .orderByAsc(LightingDiagRule::getSort)
                .orderByAsc(LightingDiagRule::getId));
    }

    @Override
    public LightingDiagRule getEnabledByCode(String ruleCode) {
        if (StringUtils.isBlank(ruleCode)) {
            return null;
        }
        List<LightingDiagRule> rules = super.list(new LambdaQueryWrapper<LightingDiagRule>()
                .eq(LightingDiagRule::getRuleCode, ruleCode)
                .eq(LightingDiagRule::getStatus, STATUS_ENABLE)
                .orderByAsc(LightingDiagRule::getId));
        return rules.isEmpty() ? null : rules.get(0);
    }
}
