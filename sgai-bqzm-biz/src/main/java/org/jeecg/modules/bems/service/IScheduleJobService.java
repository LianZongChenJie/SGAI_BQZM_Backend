package org.jeecg.modules.bems.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.entity.ScheduleJob;

public interface IScheduleJobService extends IService<ScheduleJob> {

    /**
     * 启用定时任务
     */
    void start(Long id);

    /**
     * 停用定时任务
     */
    void stop(Long id);

    /**
     * 手动执行一次定时任务
     */
    void executeOnce(Long id);
}
