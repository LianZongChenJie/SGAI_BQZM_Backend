package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitAlarm;
import org.jeecg.modules.bems.lighting.mapper.LightingCircuitAlarmMapper;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitAlarmService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LightingCircuitAlarmServiceImpl
        extends ServiceImpl<LightingCircuitAlarmMapper, LightingCircuitAlarm>
        implements ILightingCircuitAlarmService {

    @Override
    public void rebuildBySpace(String space, List<LightingCircuitAlarm> newHits) {
        // 先删该空间全部旧命中
        remove(new LambdaQueryWrapper<LightingCircuitAlarm>()
                .eq(StringUtils.isNotBlank(space), LightingCircuitAlarm::getSpace, space));
        if (newHits != null && !newHits.isEmpty()) {
            // 达梦 DM8 对 JDBC executeBatch 批量插入支持不佳(index out of range)，
            // 这里改为逐条 save。数据量小(903 至多几百条)无性能压力。
            for (LightingCircuitAlarm hit : newHits) {
                save(hit);
            }
        }
    }

    @Override
    public Map<String, Long> countByRule() {
        List<LightingCircuitAlarm> all = list(new LambdaQueryWrapper<LightingCircuitAlarm>()
                .isNotNull(LightingCircuitAlarm::getRuleCode));
        return all.stream()
                .filter(a -> a.getRuleCode() != null)
                .collect(Collectors.groupingBy(LightingCircuitAlarm::getRuleCode, Collectors.counting()));
    }

    @Override
    public List<LightingCircuitAlarm> listAll() {
        return list();
    }
}
