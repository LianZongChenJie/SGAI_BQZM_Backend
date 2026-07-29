package org.jeecg.modules.bems.energyAnalysis.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyPricingConfig;
import org.jeecg.modules.bems.energyAnalysis.mapper.EnergyPricingConfigMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IEnergyPricingConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnergyPricingConfigServiceImpl extends ServiceImpl<EnergyPricingConfigMapper, EnergyPricingConfig> implements IEnergyPricingConfigService {
    @Override
    public boolean save(EnergyPricingConfig data) {
        data.setId(null);
        data.setStatus(EnergyPricingConfig.STATUS_DISABLE);
        String category = data.getCategory();
        if (count(new LambdaQueryWrapper<EnergyPricingConfig>().eq(EnergyPricingConfig::getCategory, category)) > 0) {
            // 更新
            return update(data, new LambdaQueryWrapper<EnergyPricingConfig>().eq(EnergyPricingConfig::getCategory, category));
        } else {
            // 新增
            return super.save(data);
        }
    }

    @Override
    public void add(EnergyPricingConfig data) {
        data.setId(null);
        data.setStatus(EnergyPricingConfig.STATUS_DISABLE);
        super.save(data);
    }

    @Override
    public void update(EnergyPricingConfig data) {
        data.setStatus(null);
        data.setCategoryId(null);
        updateById(data);
    }

    private void updateStatus(Long id, String status) {
        update(new LambdaUpdateWrapper<EnergyPricingConfig>().set(EnergyPricingConfig::getStatus, status).eq(EnergyPricingConfig::getId, id));
    }

    @Override
    public void enable(Long id) {
        EnergyPricingConfig byId = getById(id);
        if(byId == null){
            throw new JeecgBootException("数据不存在");
        }
        if(count(new LambdaQueryWrapper<EnergyPricingConfig>().eq(EnergyPricingConfig::getCategoryId, byId.getCategoryId()).eq(EnergyPricingConfig::getStatus, EnergyPricingConfig.STATUS_ENABLE)) > 0){
            throw new JeecgBootException("启用失败，相同类别已存在启用");
        }
        updateStatus(id, EnergyPricingConfig.STATUS_ENABLE);
    }

    @Override
    public void disable(Long id) {
        updateStatus(id, EnergyPricingConfig.STATUS_DISABLE);
    }

    @Override
    public Page<EnergyPricingConfig> listPage(EnergyPricingConfig params) {
        return page(new Page<>(params.getPageNo(), params.getPageSize()),
                new LambdaQueryWrapper<EnergyPricingConfig>()
                        .eq(params.getCategoryId() != null, EnergyPricingConfig::getCategoryId, params.getCategoryId())
                        .orderByAsc(EnergyPricingConfig::getStatus)
                        .orderByDesc(EnergyPricingConfig::getCreateTime));
    }


    @Override
    public EnergyPricingConfig getByCategory(String category) {
        List<EnergyPricingConfig> list = list(new LambdaQueryWrapper<EnergyPricingConfig>().eq(EnergyPricingConfig::getCategory, category));
        if (CollectionUtil.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public EnergyPricingConfig getByCategoryId(Long categoryId) {
        List<EnergyPricingConfig> list = list(new LambdaQueryWrapper<EnergyPricingConfig>().eq(EnergyPricingConfig::getCategoryId, categoryId).eq(EnergyPricingConfig::getStatus, EnergyPricingConfig.STATUS_ENABLE));
        if (CollectionUtil.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }
}
