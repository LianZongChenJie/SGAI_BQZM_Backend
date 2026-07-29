package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyAnalysisConfig;
import org.jeecg.modules.bems.energyAnalysis.mapper.EnergyAnalysisConfigMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IEnergyAnalysisConfigService;
import org.jeecg.modules.bems.patterned.entity.LinkageStrategy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnergyAnalysisConfigServiceImpl extends ServiceImpl<EnergyAnalysisConfigMapper, EnergyAnalysisConfig> implements IEnergyAnalysisConfigService {
    @Override
    public void add(EnergyAnalysisConfig data) {
        if(count(new LambdaQueryWrapper<EnergyAnalysisConfig>().eq(EnergyAnalysisConfig::getName, data.getName())) > 0){
            throw new JeecgBootException("名称重复");
        }
        data.setStatus(EnergyAnalysisConfig.STATUS_DISABLE);
        save(data);
    }

    @Override
    public void update(EnergyAnalysisConfig data) {
        if(count(new LambdaQueryWrapper<EnergyAnalysisConfig>().ne(EnergyAnalysisConfig::getId,data.getId()).eq(EnergyAnalysisConfig::getName, data.getName())) > 0){
            throw new JeecgBootException("名称重复");
        }
        data.setStatus(null);
        updateById(data);
    }

    @Override
    public void enable(Long id) {
        update(new LambdaUpdateWrapper<EnergyAnalysisConfig>().eq(EnergyAnalysisConfig::getId, id).set(EnergyAnalysisConfig::getStatus, EnergyAnalysisConfig.STATUS_ENABLE));
    }

    @Override
    public void disable(Long id) {
        update(new LambdaUpdateWrapper<EnergyAnalysisConfig>().eq(EnergyAnalysisConfig::getId, id).set(EnergyAnalysisConfig::getStatus, EnergyAnalysisConfig.STATUS_DISABLE));
    }

    @Override
    public List<EnergyAnalysisConfig> list(EnergyAnalysisConfig params) {
        return list(new LambdaQueryWrapper<EnergyAnalysisConfig>()
                .eq(StringUtils.isNotEmpty(params.getStatus()),EnergyAnalysisConfig::getStatus,params.getStatus())
                .orderByAsc(EnergyAnalysisConfig::getSort));
    }

}
