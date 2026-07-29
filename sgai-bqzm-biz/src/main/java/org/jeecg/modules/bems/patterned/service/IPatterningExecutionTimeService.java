package org.jeecg.modules.bems.patterned.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.patterned.entity.PatterningExecutionTime;

import java.time.LocalDate;

public interface IPatterningExecutionTimeService extends IService<PatterningExecutionTime> {

    PatterningExecutionTime getByPatterningId(Long patterningId);

    /**
     * 获取明天需要执行的场景控制
     */
    void getNextExecution(LocalDate date);

    void getNextExecution(PatterningExecutionTime config, LocalDate date);
}
