package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.energyAnalysis.entity.CostCenterDataYear;
import org.jeecg.modules.bems.energyAnalysis.mapper.CostCenterDataYearMapper;
import org.jeecg.modules.bems.energyAnalysis.service.ICostCenterDataYearService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CostCenterDataYearServiceImpl extends ServiceImpl<CostCenterDataYearMapper, CostCenterDataYear> implements ICostCenterDataYearService {
    @Override
    public void save(String type, Long relId, LocalDateTime time, BigDecimal value, BigDecimal cost) {
        List<CostCenterDataYear> list = list(new LambdaQueryWrapper<CostCenterDataYear>().eq(CostCenterDataYear::getType, type).eq(CostCenterDataYear::getRelId, relId).eq(CostCenterDataYear::getTime, time));
        if (CollectionUtils.isEmpty(list)) {
            CostCenterDataYear data = new CostCenterDataYear();
            data.setType(type);
            data.setRelId(relId);
            data.setTime(time);
            data.setValue(value);
            data.setCost(cost);
            save(data);
        } else {
            CostCenterDataYear costCenterDataHour = list.get(0);
            costCenterDataHour.setValue(costCenterDataHour.getValue().add(value));
            costCenterDataHour.setCost(costCenterDataHour.getCost().add(cost));
            updateById(costCenterDataHour);
        }
    }

    @Override
    public List<CostCenterDataYear> listByRelTypeAndRelIdsAndTime(String relType, List<Long> relIds, LocalDateTime time) {
        return list(new LambdaQueryWrapper<CostCenterDataYear>().eq(CostCenterDataYear::getType, relType).in(CostCenterDataYear::getRelId, relIds).eq(CostCenterDataYear::getTime, time));
    }
}
