package org.jeecg.modules.bems.patterned.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.patterned.entity.PatterningPoint;
import org.jeecg.modules.bems.patterned.mapper.PatterningPointMapper;
import org.jeecg.modules.bems.patterned.service.IPatterningPointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatterningPointServiceImpl extends ServiceImpl<PatterningPointMapper, PatterningPoint> implements IPatterningPointService {
    @Override
    public void removeByPatterningStrategyId(Long patterningStrategyId) {
        remove(new LambdaQueryWrapper<PatterningPoint>().eq(PatterningPoint::getPatternStrategyId, patterningStrategyId));
    }


    @Override
    @Transactional
    public void save(Long patterningStrategyId, List<PatterningPoint> patterningPointList) {
        for(PatterningPoint patterningPoint : patterningPointList){
            patterningPoint.setId(null);
            patterningPoint.setPatternStrategyId(patterningStrategyId);
            save(patterningPoint);
        }
    }

    @Override
    public List<PatterningPoint> findByPatterningStrategyId(Long patterningStrategyId) {
        return list(new LambdaQueryWrapper<PatterningPoint>().eq(PatterningPoint::getPatternStrategyId, patterningStrategyId));
    }
}
