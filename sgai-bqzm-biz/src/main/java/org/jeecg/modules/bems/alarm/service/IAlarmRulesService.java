package org.jeecg.modules.bems.alarm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.alarm.entity.AlarmRules;

import java.util.Collection;
import java.util.List;

public interface IAlarmRulesService extends IService<AlarmRules> {

    void startRule(Long id);
    void stopRule(Long id);
    AlarmRules getDetailById(Long id);
    IPage<AlarmRules> listPage(AlarmRules params);

    List<AlarmRules> listEnabledByIds(Collection<Long> ids);
}
