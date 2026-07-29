package org.jeecg.modules.bems.bc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.bc.dto.BuildingControlPointDto;
import org.jeecg.modules.bems.bc.entity.BuildingControlPointHistory;
import org.jeecg.modules.bems.bc.mapper.BuildingControlPointHistoryMapper;
import org.jeecg.modules.bems.bc.service.IBuildingControlPointHistoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BuildingControlPointHistoryServiceImpl extends ServiceImpl<BuildingControlPointHistoryMapper, BuildingControlPointHistory> implements IBuildingControlPointHistoryService {
    @Override
    public Page<BuildingControlPointHistory> listPage(BuildingControlPointDto params) {
        return super.page(
                new Page<>(params.getPageNo(), params.getPageSize()),
                new LambdaQueryWrapper<BuildingControlPointHistory>()
                        .eq(BuildingControlPointHistory::getPointId, params.getPointId())
                        .orderByDesc(BuildingControlPointHistory::getCollectionTime)
                );
    }

    @Override
    public void save(Long pointId, String value, LocalDateTime collectionTime) {
        BuildingControlPointHistory history = new BuildingControlPointHistory();
        history.setPointId(pointId);
        history.setValue(value);
        history.setCollectionTime(collectionTime);
        super.save(history);
    }
}
