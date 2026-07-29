package org.jeecg.modules.bems.patterned.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.patterned.entity.FrontPoint;

import java.util.Collection;
import java.util.List;

public interface IFrontPointService extends IService<FrontPoint> {

    void removeByLinkageStrategyId(Long linkageStrategyId);

    List<FrontPoint> getListByLinkageStrategyId(Long linkageStrategyId);

    List<FrontPoint> getListByPointId(Long pointId);

    List<FrontPoint> getListByLinkageStrategyIds(Collection<Long> linkageStrategyIds);
}
