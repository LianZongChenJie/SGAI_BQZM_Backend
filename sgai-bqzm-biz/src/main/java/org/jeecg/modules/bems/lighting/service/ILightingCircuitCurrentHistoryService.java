package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitCurrentHistory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回路电流整点采样历史服务
 */
public interface ILightingCircuitCurrentHistoryService extends IService<LightingCircuitCurrentHistory> {

    /**
     * 立即执行一次整点采样（定时任务与手工补采共用）。
     * <p>
     * 采样时刻取当前整点（分秒清零）；同一整点已采过则直接返回 0，不会重复写。
     *
     * @return 实际写入条数
     */
    int snapshotNow();

    /**
     * 判断某个整点是否已经采过（防止重复落库，任务重跑/时钟回拨都不会写两批）
     */
    boolean existsAt(LocalDateTime collectTime);

    /**
     * 查询回路电流采样历史
     *
     * @param circuitId 回路ID（可空，空则不按回路过滤）
     * @param areaId    区域ID（可空）
     * @param start     开始时间（可空，含）
     * @param end       结束时间（可空，含）
     * @param limit     最大返回条数（默认 500，按时间升序返回）
     */
    List<LightingCircuitCurrentHistory> queryHistory(Long circuitId, Long areaId,
                                                     LocalDateTime start, LocalDateTime end,
                                                     Integer limit);
}
