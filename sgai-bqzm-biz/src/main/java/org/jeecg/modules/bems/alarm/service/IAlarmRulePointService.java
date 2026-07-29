package org.jeecg.modules.bems.alarm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.alarm.entity.AlarmRulePoint;

import java.util.Collection;
import java.util.List;

public interface IAlarmRulePointService extends IService<AlarmRulePoint> {

    void save(Long alarmRuleId, List<AlarmRulePoint> alarmRulePoints);

    void removeByAlarmRuleId(Long alarmRuleId);

    List<AlarmRulePoint> getByAlarmRuleId(Long alarmRuleId);

    List<AlarmRulePoint> getByAlarmRuleIds(Collection<Long> alarmRuleIds);

    List<AlarmRulePoint> getByDeviceIdAndPointId(Long deviceId,Long pointId);

    List<AlarmRulePoint> getByDeviceIdAndTimeGranularity(Long deviceId,String timeGranularity);

    /**
     * 获取告警类型是时间粒度的规则
     * @param deviceId 设备id
     */
    List<AlarmRulePoint> getTimeGranularityByDeviceId(Long deviceId);
}
