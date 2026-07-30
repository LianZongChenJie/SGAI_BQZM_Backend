package org.jeecg.modules.bems.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.entity.ScheduleJob;
import org.jeecg.modules.bems.job.DynamicScheduleManager;
import org.jeecg.modules.bems.mapper.ScheduleJobMapper;
import org.jeecg.modules.bems.service.IScheduleJobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ScheduleJobServiceImpl extends ServiceImpl<ScheduleJobMapper, ScheduleJob> implements IScheduleJobService {

    private final DynamicScheduleManager dynamicScheduleManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long id) {
        ScheduleJob job = getById(id);
        if (job == null) {
            throw new RuntimeException("定时任务不存在，id: " + id);
        }
        job.setStatus(1);
        updateById(job);
        dynamicScheduleManager.addJob(job);
        log.info("定时任务已启用: {}", job.getJobName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stop(Long id) {
        ScheduleJob job = getById(id);
        if (job == null) {
            throw new RuntimeException("定时任务不存在，id: " + id);
        }
        job.setStatus(0);
        updateById(job);
        dynamicScheduleManager.removeJob(job.getId());
        log.info("定时任务已停用: {}", job.getJobName());
    }

    @Override
    public void executeOnce(Long id) {
        ScheduleJob job = getById(id);
        if (job == null) {
            throw new RuntimeException("定时任务不存在，id: " + id);
        }
        dynamicScheduleManager.executeJob(job);
    }
}
