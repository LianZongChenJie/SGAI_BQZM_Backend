package org.jeecg.modules.bems.alarm.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.alarm.entity.AlarmRulePoint;
import org.jeecg.modules.bems.alarm.mapper.AlarmRulePointMapper;
import org.jeecg.modules.bems.alarm.service.IAlarmRulePointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class AlarmRulePointServiceImpl extends ServiceImpl<AlarmRulePointMapper, AlarmRulePoint> implements IAlarmRulePointService {
    @Override
    @Transactional
    public void save(Long alarmRuleId, List<AlarmRulePoint> alarmRulePoints) {
        // 删除原有点位信息
        removeByAlarmRuleId(alarmRuleId);
        for (AlarmRulePoint alarmRulePoint : alarmRulePoints) {
            alarmRulePoint.setId(null);
            alarmRulePoint.setAlarmRuleId(alarmRuleId);
            save(alarmRulePoint);
        }
    }

    @Override
    public void removeByAlarmRuleId(Long alarmRuleId) {
        remove(new LambdaQueryWrapper<AlarmRulePoint>().eq(AlarmRulePoint::getAlarmRuleId, alarmRuleId));
    }

    @Override
    public List<AlarmRulePoint> getByAlarmRuleId(Long alarmRuleId) {
        return list(new LambdaQueryWrapper<AlarmRulePoint>().eq(AlarmRulePoint::getAlarmRuleId, alarmRuleId));
    }

    @Override
    public List<AlarmRulePoint> getByAlarmRuleIds(Collection<Long> alarmRuleIds) {
        if(CollectionUtil.isEmpty(alarmRuleIds)) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<AlarmRulePoint>().in(AlarmRulePoint::getAlarmRuleId,alarmRuleIds));
    }

    @Override
    public List<AlarmRulePoint> getByDeviceIdAndPointId(Long deviceId, Long pointId) {
        if(deviceId == null || pointId == null){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<AlarmRulePoint>().eq(AlarmRulePoint::getDeviceId, deviceId).eq(AlarmRulePoint::getPointId, pointId));
    }

    @Override
    public List<AlarmRulePoint> getByDeviceIdAndTimeGranularity(Long deviceId, String timeGranularity) {
        if(deviceId == null || timeGranularity == null){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<AlarmRulePoint>().eq(AlarmRulePoint::getDeviceId, deviceId).eq(AlarmRulePoint::getTimeGranularity, timeGranularity));
    }

    /**
     * 获取告警类型是时间粒度的规则
     *
     * @param deviceId 设备id
     */
    @Override
    public List<AlarmRulePoint> getTimeGranularityByDeviceId(Long deviceId) {
        if(deviceId == null){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<AlarmRulePoint>().eq(AlarmRulePoint::getDeviceId, deviceId).isNotNull(AlarmRulePoint::getTimeGranularity));
    }
}
