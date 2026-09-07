package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitAlarm;

import java.util.List;
import java.util.Map;

public interface ILightingCircuitAlarmService extends IService<LightingCircuitAlarm> {

    /**
     * 重建指定空间下的当前命中（先删该空间所有命中，再批量插入 newHits）
     */
    void rebuildBySpace(String space, List<LightingCircuitAlarm> newHits);

    /**
     * 各规则命中回路数：rule_code -> count（按 circuit_id 去重统计）
     */
    Map<String, Long> countByRule();

    /**
     * 当前命中明细（可含全部空间）
     */
    List<LightingCircuitAlarm> listAll();
}
