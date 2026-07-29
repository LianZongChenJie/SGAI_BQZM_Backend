package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyAnalysisBenchmark;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyAnalysisConfig;
import org.jeecg.modules.bems.energyAnalysis.mapper.EnergyAnalysisBenchmarkMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IEnergyAnalysisBenchmarkService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnergyAnalysisBenchmarkServiceImpl extends ServiceImpl<EnergyAnalysisBenchmarkMapper,EnergyAnalysisBenchmark> implements IEnergyAnalysisBenchmarkService {
    @Override
    public void add(EnergyAnalysisBenchmark data) {
        if(data.getConfigId() == null){
            throw new JeecgBootException("参数异常！");
        }
        save(data);
    }

    @Override
    public void update(EnergyAnalysisBenchmark data) {
        data.setConfigId(null);
        updateById(data);
    }

    @Override
    public void delete(Long id){
        removeById(id);
    }

    @Override
    public List<EnergyAnalysisBenchmark> list(EnergyAnalysisBenchmark params) {
        return list(new LambdaQueryWrapper<EnergyAnalysisBenchmark>().eq(EnergyAnalysisBenchmark::getConfigId,params.getConfigId()).orderByAsc(EnergyAnalysisBenchmark::getSort));
    }
}
