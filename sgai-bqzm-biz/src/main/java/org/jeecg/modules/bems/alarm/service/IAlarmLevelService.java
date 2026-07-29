package org.jeecg.modules.bems.alarm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.alarm.entity.AlarmLevel;

public interface IAlarmLevelService extends IService<AlarmLevel> {

    IPage<AlarmLevel> listPage(AlarmLevel params);

    void startLevel(Long id);

    void stopLevel(Long id);
}
