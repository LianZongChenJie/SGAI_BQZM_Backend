package org.jeecg.modules.bems.patterned.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.modules.bems.patterned.entity.PatterningExecutionTime;
import org.jeecg.modules.bems.patterned.mapper.PatterningExecutionTimeMapper;
import org.jeecg.modules.bems.patterned.mq.send.PatterningMqSendService;
import org.jeecg.modules.bems.patterned.service.IPatterningExecutionTimeService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@AllArgsConstructor
public class PatterningExecutionTimeServiceImpl extends ServiceImpl<PatterningExecutionTimeMapper, PatterningExecutionTime> implements IPatterningExecutionTimeService {

    private final PatterningMqSendService patterningMqSendService;

    @Override
    public PatterningExecutionTime getByPatterningId(Long patterningId) {
        return getOne(new LambdaQueryWrapper<PatterningExecutionTime>().eq(PatterningExecutionTime::getPatterningId, patterningId));
    }

    /**
     * 获取明天需要执行的场景控制
     */
    @Override
    public void getNextExecution(LocalDate date){
        // 获取周几
        String week = String.valueOf(date.getDayOfWeek().getValue());
        List<PatterningExecutionTime> list = super.list();

        for (PatterningExecutionTime item : list) {
            // 修复日期判断逻辑
            if (date.isBefore(item.getBeginDate()) || date.isAfter(item.getEndDate())) {
                continue;
            }
            if(!item.getEnabledWeek().contains(week)){
                continue;
            }
            // 执行时间
            LocalDateTime executionTime = date.atTime(item.getBeginTime());
            // 放入延迟队列
            patterningMqSendService.sendPatterningDelayMessage(item.getPatterningId(), item.getVersion(), executionTime);
        }
    }

    @Override
    public void getNextExecution(PatterningExecutionTime config, LocalDate date) {
        String week = String.valueOf(date.getDayOfWeek().getValue());
        // 修复日期判断逻辑
        if (date.isBefore(config.getBeginDate()) || date.isAfter(config.getEndDate())) {
            return;
        }
        if(!config.getEnabledWeek().contains(week)){
            return;
        }
        // 执行时间
        LocalDateTime executionTime = date.atTime(config.getBeginTime());
        // 放入延迟队列
        patterningMqSendService.sendPatterningDelayMessage(config.getPatterningId(), config.getVersion(), executionTime);
    }


}
