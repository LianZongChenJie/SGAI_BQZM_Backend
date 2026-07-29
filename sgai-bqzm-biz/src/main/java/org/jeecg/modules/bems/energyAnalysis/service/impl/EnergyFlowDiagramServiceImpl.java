package org.jeecg.modules.bems.energyAnalysis.service.impl;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointData;
import org.jeecg.modules.bems.energyAnalysis.service.*;
import org.jeecg.modules.bems.entity.UnitManagement;
import org.jeecg.modules.bems.service.*;
import org.jeecg.modules.bems.energyAnalysis.vo.EnergyFlowDiagramVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EnergyFlowDiagramServiceImpl implements IEnergyFlowDiagramService {
    private final IMeteringPointService pointService;

    private final IMeteringPointDataDayService dayService;
    private final IMeteringPointDataMonthService monthService;
    private final IMeteringPointDataYearService yearService;
    private final IUnitManagementService unitService;

    @Override
    public List<EnergyFlowDiagramVo> findDay(String type, LocalDate date) {
        // 获取配置
        List<MeteringPoint> configs = pointService.listByType(type);
        // 获取时间范围内的数据
        List<Long> meteringPointId = configs.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        return convert(configs, dayService.findByDateAndPointIds(date, meteringPointId));
    }

    @Override
    public List<EnergyFlowDiagramVo> findMonth(String type, LocalDate date) {
        // 获取配置
        List<MeteringPoint> configs = pointService.listByType(type);
        // 获取时间范围内的数据
        List<Long> meteringPointId = configs.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        return convert(configs, monthService.findByDateAndPointIds(date, meteringPointId));
    }

    @Override
    public List<EnergyFlowDiagramVo> findYear(String type, LocalDate date) {
        // 获取配置
        List<MeteringPoint> configs = pointService.listByType(type);
        // 获取时间范围内的数据
        List<Long> meteringPointId = configs.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        return convert(configs, yearService.findByDateAndPointIds(date, meteringPointId));
    }

    @Override
    public List<EnergyFlowDiagramVo> findDay(Long pointId,Integer level, LocalDate date) {
        List<MeteringPoint> list = pointService.getTreeListByIdAndLevel(pointId,level);
        List<Long> meteringPointId = list.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        return convert(list, dayService.findByDateAndPointIds(date, meteringPointId));
    }

    @Override
    public List<EnergyFlowDiagramVo> findMonth(Long pointId,Integer level, LocalDate date) {
        List<MeteringPoint> list = pointService.getTreeListByIdAndLevel(pointId,level);
        List<Long> meteringPointId = list.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        return convert(list, monthService.findByDateAndPointIds(date, meteringPointId));
    }

    @Override
    public List<EnergyFlowDiagramVo> findYear(Long pointId,Integer level, LocalDate date) {
        List<MeteringPoint> list = pointService.getTreeListByIdAndLevel(pointId,level);
        List<Long> meteringPointId = list.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        return convert(list, yearService.findByDateAndPointIds(date, meteringPointId));
    }

    private List<EnergyFlowDiagramVo> convert(List<MeteringPoint> configs, List<? extends MeteringPointData> dataList) {
        Map<Long, BigDecimal> dataMap = dataList
                .stream().collect(Collectors.toMap(MeteringPointData::getMeteringPointId, MeteringPointData::getValue, (v1, v2) -> v1));
        // 获取计量单位信息
        Map<Long,UnitManagement> unitMap = unitService.list().stream().collect(Collectors.toMap(UnitManagement::getId, Function.identity()));
        List<EnergyFlowDiagramVo> res = new ArrayList<>();
        Map<Long, String> nodeNameMap = configs.stream().collect(Collectors.toMap(MeteringPoint::getId, MeteringPoint::getNodeName));
//        configs.sort(Comparator.comparing(MeasureRule::getSort));
        // 同一个节点下的节点，按顺序排序
        List<Long> ids = new ArrayList<>();
        for (MeteringPoint config : configs) {
            List<MeteringPoint> children = configs.stream().filter(c -> c.getParentId().equals(config.getId())).sorted(Comparator.comparing(MeteringPoint::getSort)).collect(Collectors.toList());
            if (!ids.contains(config.getId())) {
                UnitManagement unit = unitMap.getOrDefault(config.getMeteringUnit(),new UnitManagement());
                res.add(new EnergyFlowDiagramVo(config.getId(), config.getParentId(), nodeNameMap.getOrDefault(config.getParentId(), ""), config.getType(), config.getNodeName(), dataMap.getOrDefault(config.getId(), BigDecimal.ZERO),unit.getEnglishAme(),unit.getName(), StringUtils.isEmpty(config.getFormula()) ? "0" : "1"));
                ids.add(config.getId());
            }
            for (MeteringPoint child : children) {
                if (!ids.contains(child.getId())) {
                    UnitManagement unit = unitMap.getOrDefault(child.getMeteringUnit(),new UnitManagement());
                    ids.add(child.getId());
                    res.add(new EnergyFlowDiagramVo(child.getId(), child.getParentId(), nodeNameMap.getOrDefault(child.getParentId(), ""), child.getType(), child.getNodeName(), dataMap.getOrDefault(child.getId(), BigDecimal.ZERO),unit.getEnglishAme(),unit.getName(),StringUtils.isEmpty(config.getFormula()) ? "0" : "1"));
                }
            }
        }
        return res;
    }
}
