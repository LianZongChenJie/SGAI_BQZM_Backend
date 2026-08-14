package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingEnergyHour;

import java.time.LocalDateTime;

public interface ILightingEnergyHourService extends IService<LightingEnergyHour> {

    /**
     * 聚合指定小时的用电量（整点任务调用）：
     * 对每个表计（网关+回路），用"该小时末条累计读数 - 上小时末条累计读数"计算本小时用电量，
     * 写入 lighting_energy_hour（幂等：同表计同小时先删后插）。
     *
     * @param hourStart 小时起始时间（如 10:00，统计 10:00~11:00 的用电量）
     */
    void aggregateHour(LocalDateTime hourStart);
}
