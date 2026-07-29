package org.jeecg.modules.bems.patterned.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.patterned.entity.PointExecuteRecord;
import org.jeecg.modules.bems.patterned.mapper.PointExecuteRecordMapper;
import org.jeecg.modules.bems.patterned.service.IPointExecuteRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointExecuteRecordServiceImpl extends ServiceImpl<PointExecuteRecordMapper, PointExecuteRecord> implements IPointExecuteRecordService {
    @Override
    public List<PointExecuteRecord> getByStrategyExecuteId(Long strategyExecuteId) {
        return list(new LambdaQueryWrapper<PointExecuteRecord>().eq(PointExecuteRecord::getStrategyExecuteId,strategyExecuteId));
    }
}
