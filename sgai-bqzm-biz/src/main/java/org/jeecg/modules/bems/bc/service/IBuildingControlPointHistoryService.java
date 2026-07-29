package org.jeecg.modules.bems.bc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.bc.dto.BuildingControlPointDto;
import org.jeecg.modules.bems.bc.entity.BuildingControlPointHistory;

import java.time.LocalDateTime;

public interface IBuildingControlPointHistoryService extends IService<BuildingControlPointHistory> {

    Page<BuildingControlPointHistory> listPage(BuildingControlPointDto params);

    void save(Long pointId, String value, LocalDateTime collectionTime);
}
