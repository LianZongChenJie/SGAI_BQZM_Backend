package org.jeecg.modules.bems.energyAnalysis.service;

import org.jeecg.modules.bems.energyAnalysis.vo.EnergyFlowDiagramVo;

import java.time.LocalDate;
import java.util.List;

public interface IEnergyFlowDiagramService {

    List<EnergyFlowDiagramVo> findDay(String type, LocalDate date);

    List<EnergyFlowDiagramVo> findMonth(String type, LocalDate date);

    List<EnergyFlowDiagramVo> findYear(String type, LocalDate date);

    List<EnergyFlowDiagramVo> findDay(Long pointId,Integer level, LocalDate date);

    List<EnergyFlowDiagramVo> findMonth(Long pointId,Integer level, LocalDate date);

    List<EnergyFlowDiagramVo> findYear(Long pointId,Integer level, LocalDate date);
}
