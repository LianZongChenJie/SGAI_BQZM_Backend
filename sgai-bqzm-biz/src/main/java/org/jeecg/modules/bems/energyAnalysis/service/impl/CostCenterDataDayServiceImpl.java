package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.energyAnalysis.entity.CostCenterDataDay;
import org.jeecg.modules.bems.energyAnalysis.mapper.CostCenterDataDayMapper;
import org.jeecg.modules.bems.energyAnalysis.service.ICostCenterDataDayService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CostCenterDataDayServiceImpl extends ServiceImpl<CostCenterDataDayMapper, CostCenterDataDay> implements ICostCenterDataDayService {
    @Override
    public void save(String type, Long relId, LocalDateTime time, BigDecimal value, BigDecimal cost) {
        List<CostCenterDataDay> list =list(new LambdaQueryWrapper<CostCenterDataDay>().eq(CostCenterDataDay::getType, type).eq(CostCenterDataDay::getRelId, relId).eq(CostCenterDataDay::getTime, time));
        if(CollectionUtils.isEmpty(list)){
            CostCenterDataDay data = new CostCenterDataDay();
            data.setType(type);
            data.setRelId(relId);
            data.setTime(time);
            data.setValue(value);
            data.setCost(cost);
            save(data);
        }else{
            CostCenterDataDay costCenterDataHour = list.get(0);
            costCenterDataHour.setValue(costCenterDataHour.getValue().add(value));
            costCenterDataHour.setCost(costCenterDataHour.getCost().add(cost));
            updateById(costCenterDataHour);
        }
    }

    @Override
    public List<CostCenterDataDay> listByRelTypeAndRelIdsAndTime(String type, List<Long> relIds, LocalDateTime time) {
        return list(new LambdaQueryWrapper<CostCenterDataDay>().eq(CostCenterDataDay::getType, type).in(CostCenterDataDay::getRelId, relIds).eq(CostCenterDataDay::getTime, time));
    }
}
