package org.jeecg.modules.bems.alarm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.alarm.entity.AlarmCategory;

public interface IAlarmCategoryService extends IService<AlarmCategory> {

    IPage<AlarmCategory> listPage(AlarmCategory params);

    void startCategory(Long id);

    void stopCategory(Long id);
}
