package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyAnalysisConfig;

import java.util.List;

public interface IEnergyAnalysisConfigService extends IService<EnergyAnalysisConfig> {

    void add(EnergyAnalysisConfig data);

    void update(EnergyAnalysisConfig data);

    void enable(Long id);

    void disable(Long id);

    List<EnergyAnalysisConfig> list(EnergyAnalysisConfig params);

}
