package org.jeecg.modules.bems.patterned.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.patterned.entity.FrontPoint;
import org.jeecg.modules.bems.patterned.mapper.FrontPointMapper;
import org.jeecg.modules.bems.patterned.service.IFrontPointService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class FrontPointServiceImpl extends ServiceImpl<FrontPointMapper, FrontPoint> implements IFrontPointService {
    @Override
    public void removeByLinkageStrategyId(Long linkageStrategyId) {
        remove(new LambdaQueryWrapper<FrontPoint>().eq(FrontPoint::getLinkageStrategyId, linkageStrategyId));
    }

    @Override
    public List<FrontPoint> getListByLinkageStrategyId(Long linkageStrategyId) {
        return list(new LambdaQueryWrapper<FrontPoint>().eq(FrontPoint::getLinkageStrategyId, linkageStrategyId));
    }

    @Override
    public List<FrontPoint> getListByPointId(Long pointId) {
        return list(new LambdaQueryWrapper<FrontPoint>().eq(FrontPoint::getPointId, pointId));
    }

    @Override
    public List<FrontPoint> getListByLinkageStrategyIds(Collection<Long> linkageStrategyIds) {
        return list(new LambdaQueryWrapper<FrontPoint>().in(FrontPoint::getLinkageStrategyId, linkageStrategyIds));
    }
}
