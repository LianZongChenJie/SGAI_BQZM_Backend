package org.jeecg.modules.bems.patterned.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.patterned.entity.PatterningExecutionTime;
import org.jeecg.modules.bems.patterned.entity.PatterningStrategy;

import java.time.LocalDateTime;

public interface IPatterningStrategyService extends IService<PatterningStrategy> {

    PatterningStrategy getDetailById(Long id);

    void deleteById(Long id);

    /**
     * 启用场景控制
     */
    void startStrategy(PatterningExecutionTime data);

    /**
     * 禁用场景控制
     * @param id 场景控制id
     */
    void stopStrategy(Long id);

    /**
     * 发送设备指令
     */
    void executeImmediately(Long id);

    Page<PatterningStrategy> listPage(PatterningStrategy params);

    /**
     * 定时执行场景控制
     * @param id 场景控制id
     * @param executeTime 执行时间
     */
    void executeImmediately(Long id, LocalDateTime executeTime);
}
