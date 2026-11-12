package org.jeecg.modules.bems.lighting.job;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitCurrentHistoryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 回路电流整点采样任务（2026-11-12 新增）。
 * <p>
 * 背景：{@code lighting_circuit.electric_current} 是覆盖式最新值，回路电流没有历史，
 * 被下一条 MQ 电流报文（DataType=1）覆盖后就查不到过去了。本任务每小时整点把 903 全部回路
 * 当时的电流快照落一行到 {@code lighting_circuit_current_history}，供趋势图与事后追溯。
 * <p>
 * 口径：仅 903（只有这个空间的回路有 electric_current）；粒度 1 小时，同一整点只落一批；
 * 电流为 null 的回路跳过；暂不做历史清理（用户 2026-11-12 明确"先不清理"）。
 * <p>
 * 具体采样逻辑在 {@link ILightingCircuitCurrentHistoryService#snapshotNow()}，
 * 手工补采走 {@code POST /bems/lighting/circuitCurrent/snapshot}。
 */
@Component
@AllArgsConstructor
@Slf4j
public class LightingCircuitCurrentSnapshotJob {

    private final ILightingCircuitCurrentHistoryService historyService;

    /**
     * 每小时整点 0 分 0 秒执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void runSnapshot() {
        try {
            int saved = historyService.snapshotNow();
            if (saved > 0) {
                log.info("【电流采样】整点采样完成，写入 {} 条", saved);
            }
        } catch (Exception e) {
            log.error("【电流采样】整点采样任务异常", e);
        }
    }
}
