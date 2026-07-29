package org.jeecg.modules.bems.patterned.job;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
@Data
public class PatterningEnabledCronJob implements Job {

    private String parameter;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.error("场景控制定时任务。。。场景控制id：{}", parameter);
    }
}
