package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.energyAnalysis.entity.CostCenterDataHour;
import org.jeecg.modules.bems.energyAnalysis.mapper.CostCenterDataHourMapper;
import org.jeecg.modules.bems.energyAnalysis.service.ICostCenterDataHourService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CostCenterDataHourServiceImpl extends ServiceImpl<CostCenterDataHourMapper, CostCenterDataHour> implements ICostCenterDataHourService {
    @Override
    public void save(String type, Long relId, LocalDateTime time, BigDecimal value, BigDecimal cost) {
        // 判断点位时间是否存在
        List<CostCenterDataHour> list = list(new LambdaQueryWrapper<CostCenterDataHour>().eq(CostCenterDataHour::getType, type).eq(CostCenterDataHour::getRelId, relId).eq(CostCenterDataHour::getTime, time));
        if(CollectionUtils.isEmpty(list)){
            CostCenterDataHour data = new CostCenterDataHour();
            data.setType(type);
            data.setRelId(relId);
            data.setTime(time);
            data.setValue(value);
            data.setCost(cost);
            save(data);
        }else{
            CostCenterDataHour costCenterDataHour = list.get(0);
            costCenterDataHour.setValue(value);
            costCenterDataHour.setCost(cost);
            updateById(costCenterDataHour);
        }

    }
}
