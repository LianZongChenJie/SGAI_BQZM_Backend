package org.jeecg.modules.bems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.entity.GatherRuleConfig;
import org.jeecg.modules.bems.mapper.GatherRuleConfigMapper;
import org.jeecg.modules.bems.service.IGatherRuleConfigService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 采集管理-规则标准
 * @Author: jeecg-boot
 * @Date:   2025-02-19
 * @Version: V1.0
 */
@Service
public class GatherRuleConfigServiceImpl extends ServiceImpl<GatherRuleConfigMapper, GatherRuleConfig> implements IGatherRuleConfigService {

    @Override
    public boolean save(GatherRuleConfig entity) {
        // 校验编号是否存在
        if(count(new LambdaQueryWrapper<GatherRuleConfig>().eq(GatherRuleConfig::getGatewayCode, entity.getGatewayCode())) > 0){
            throw new JeecgBootException("网关编号重复！");
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(GatherRuleConfig entity) {
        // 校验编号是否存在
        if(count(new LambdaQueryWrapper<GatherRuleConfig>().eq(GatherRuleConfig::getGatewayCode, entity.getGatewayCode()).ne(GatherRuleConfig::getId, entity.getId())) > 0){
            throw new JeecgBootException("网关编号重复！");
        }
        return super.updateById(entity);
    }
}
