package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyAnalysisChart;
import org.jeecg.modules.bems.energyAnalysis.mapper.EnergyAnalysisChartMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IEnergyAnalysisChartService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class EnergyAnalysisChartServiceImpl extends ServiceImpl<EnergyAnalysisChartMapper, EnergyAnalysisChart> implements IEnergyAnalysisChartService {
    @Override
    public void add(EnergyAnalysisChart data) {
        // 判断名称是否重复
        if(data.getConfigId() == null){
            throw new JeecgBootException("参数异常！");
        }
        if(count(new LambdaQueryWrapper<EnergyAnalysisChart>()
                .eq(EnergyAnalysisChart::getConfigId, data.getConfigId())
                .eq(EnergyAnalysisChart::getChartName, data.getChartName())) > 0){
            throw new JeecgBootException("名称重复");
        }
        save(data);
    }

    @Override
    public void update(EnergyAnalysisChart data) {
        if(count(new LambdaQueryWrapper<EnergyAnalysisChart>().eq(EnergyAnalysisChart::getConfigId,data.getConfigId()).eq(EnergyAnalysisChart::getChartName, data.getChartName()).ne(EnergyAnalysisChart::getId, data.getId())) > 0){
            throw new JeecgBootException("名称重复");
        }
        updateById(data);
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public List<EnergyAnalysisChart> list(EnergyAnalysisChart params) {
        if(params.getConfigId() == null){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<EnergyAnalysisChart>().eq(EnergyAnalysisChart::getConfigId, params.getConfigId()).orderByAsc(EnergyAnalysisChart::getSort));
    }
}
