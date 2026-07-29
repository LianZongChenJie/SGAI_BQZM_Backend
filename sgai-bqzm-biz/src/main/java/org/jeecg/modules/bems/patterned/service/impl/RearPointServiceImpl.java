package org.jeecg.modules.bems.patterned.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.patterned.entity.RearPoint;
import org.jeecg.modules.bems.patterned.mapper.RearPointMapper;
import org.jeecg.modules.bems.patterned.service.IRearPointService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class RearPointServiceImpl extends ServiceImpl<RearPointMapper, RearPoint> implements IRearPointService {
    @Override
    public void removeByLinkageStrategyId(Long linkageStrategyId) {
        remove(new LambdaQueryWrapper<RearPoint>().eq(RearPoint::getLinkageStrategyId, linkageStrategyId));
    }

    @Override
    public List<RearPoint> getListByLinkageStrategyId(Long linkageStrategyId) {
        return list(new LambdaQueryWrapper<RearPoint>().eq(RearPoint::getLinkageStrategyId, linkageStrategyId));
    }

    @Override
    public List<RearPoint> getListByLinkageStrategyIds(Collection<Long> linkageStrategyIds) {
        return list(new LambdaQueryWrapper<RearPoint>().in(RearPoint::getLinkageStrategyId, linkageStrategyIds));
    }
}
