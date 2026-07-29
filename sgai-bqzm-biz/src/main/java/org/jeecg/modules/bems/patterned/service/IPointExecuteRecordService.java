package org.jeecg.modules.bems.patterned.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.patterned.entity.PointExecuteRecord;

import java.util.List;

public interface IPointExecuteRecordService extends IService<PointExecuteRecord> {

    List<PointExecuteRecord> getByStrategyExecuteId(Long strategyExecuteId);
}
