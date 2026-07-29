package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.energyAnalysis.entity.CostCenterDataMonth;
import org.jeecg.modules.bems.energyAnalysis.mapper.CostCenterDataMonthMapper;
import org.jeecg.modules.bems.energyAnalysis.service.ICostCenterDataMonthService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CostCenterDataMonthServiceImpl extends ServiceImpl<CostCenterDataMonthMapper, CostCenterDataMonth> implements ICostCenterDataMonthService {
    @Override
    public void save(String type, Long relId, LocalDateTime time, BigDecimal value, BigDecimal cost) {
        List<CostCenterDataMonth> list = list(new LambdaQueryWrapper<CostCenterDataMonth>().eq(CostCenterDataMonth::getType, type).eq(CostCenterDataMonth::getRelId, relId).eq(CostCenterDataMonth::getTime, time));
        if(CollectionUtils.isEmpty(list)){
            CostCenterDataMonth data = new CostCenterDataMonth();
            data.setType(type);
            data.setRelId(relId);
            data.setTime(time);
            data.setValue(value);
            data.setCost(cost);
            save(data);
        }else{
            CostCenterDataMonth costCenterDataHour = list.get(0);
            costCenterDataHour.setValue(costCenterDataHour.getValue().add(value));
            costCenterDataHour.setCost(costCenterDataHour.getCost().add(cost));
            updateById(costCenterDataHour);
        }
    }

    @Override
    public List<CostCenterDataMonth> listByRelTypeAndRelIdsAndTime(String relType, List<Long> relIds, LocalDateTime time) {
        return list(new LambdaQueryWrapper<CostCenterDataMonth>().eq(CostCenterDataMonth::getType,relType).in(CostCenterDataMonth::getRelId,relIds).eq(CostCenterDataMonth::getTime,time));
    }
}
