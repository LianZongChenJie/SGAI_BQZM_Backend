package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyAnalysisBenchmark;

import java.util.List;

public interface IEnergyAnalysisBenchmarkService extends IService<EnergyAnalysisBenchmark> {

    void add(EnergyAnalysisBenchmark data);
    void update(EnergyAnalysisBenchmark data);
    void delete(Long id);
    List<EnergyAnalysisBenchmark> list(EnergyAnalysisBenchmark params);
}
