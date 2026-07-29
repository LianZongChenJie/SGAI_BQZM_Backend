package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.CostCenterDataDay;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ICostCenterDataDayService extends IService<CostCenterDataDay> {
    void save(String type, Long relId, LocalDateTime time, BigDecimal value, BigDecimal cost);

    List<CostCenterDataDay> listByRelTypeAndRelIdsAndTime(String type, List<Long> relIds, LocalDateTime time);
}
