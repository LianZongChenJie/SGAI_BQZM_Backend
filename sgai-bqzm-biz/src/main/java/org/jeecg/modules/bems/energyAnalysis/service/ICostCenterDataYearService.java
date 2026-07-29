package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.CostCenterDataYear;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ICostCenterDataYearService extends IService<CostCenterDataYear> {
    void save(String type, Long relId, LocalDateTime time, BigDecimal value, BigDecimal cost);

    List<CostCenterDataYear> listByRelTypeAndRelIdsAndTime(String relType, List<Long> relIds, LocalDateTime time);
}
