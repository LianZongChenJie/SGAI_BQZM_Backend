package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitCurrentHistory;
import org.jeecg.modules.bems.lighting.mapper.LightingCircuitCurrentHistoryMapper;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitCurrentHistoryService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class LightingCircuitCurrentHistoryServiceImpl
        extends ServiceImpl<LightingCircuitCurrentHistoryMapper, LightingCircuitCurrentHistory>
        implements ILightingCircuitCurrentHistoryService {

    /** 北区公区空间编码（唯一有回路电流的空间） */
    private static final String SPACE_BQ = "903";

    private final ILightingCircuitService circuitService;
    private final ILightingAreaService areaService;

    @Override
    public int snapshotNow() {
        // 采样时刻取整点（分秒清零）：同一整点只落一批
        LocalDateTime collectTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        if (existsAt(collectTime)) {
            log.info("【电流采样】{} 已采样过，跳过本次", collectTime);
            return 0;
        }

        List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getSpace, SPACE_BQ));
        if (CollectionUtil.isEmpty(areas)) {
            log.warn("【电流采样】未找到 space={} 的区域，跳过", SPACE_BQ);
            return 0;
        }
        Map<Long, LightingArea> areaMap = areas.stream()
                .collect(Collectors.toMap(LightingArea::getId, Function.identity(), (a, b) -> a));

        List<LightingCircuit> circuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                .in(LightingCircuit::getAreaId, areaMap.keySet()));
        if (CollectionUtil.isEmpty(circuits)) {
            log.warn("【电流采样】space={} 下没有回路，跳过", SPACE_BQ);
            return 0;
        }

        // lighting_circuit 无主键/无唯一索引（历史上出现过重复 id）→ 按 id 去重，保留第一条
        Map<Long, LightingCircuit> unique = new LinkedHashMap<>();
        for (LightingCircuit c : circuits) {
            if (c.getId() == null) {
                continue;
            }
            LightingCircuit old = unique.putIfAbsent(c.getId(), c);
            if (old != null) {
                log.warn("【电流采样】回路 id 重复，已忽略后一条：id={}, code={}", c.getId(), c.getCircuitCode());
            }
        }

        LocalDateTime createTime = LocalDateTime.now();
        List<LightingCircuitCurrentHistory> rows = new ArrayList<>(unique.size());
        int nullCurrent = 0;
        for (LightingCircuit c : unique.values()) {
            if (c.getElectricCurrent() == null) {
                nullCurrent++;
                continue;
            }
            LightingArea area = areaMap.get(c.getAreaId());
            rows.add(new LightingCircuitCurrentHistory()
                    .setCircuitId(c.getId())
                    .setCircuitName(c.getCircuitName())
                    .setCircuitCode(c.getCircuitCode())
                    .setAreaId(c.getAreaId())
                    .setAreaCode(c.getAreaCode() != null ? c.getAreaCode()
                            : (area == null ? null : area.getAreaCode()))
                    .setAreaName(area == null ? null : area.getAreaName())
                    .setDistrictId(area == null ? null : area.getDistrictId())
                    .setSpace(SPACE_BQ)
                    .setStatus(c.getStatus())
                    .setElectricCurrent(c.getElectricCurrent())
                    .setRatedElectricCurrent(c.getRatedElectricCurrent())
                    .setCollectTime(collectTime)
                    .setCreateTime(createTime));
        }

        if (rows.isEmpty()) {
            log.warn("【电流采样】{} 没有可采样的电流值（回路 {} 条全部为空），跳过", collectTime, unique.size());
            return 0;
        }

        // ⚠️ 不能走 saveBatch：达梦驱动 8.1.3.140 的"大批量"通道（DmdbPreparedStatement.do_executeLargeBatch）
        //    在 batch 条数较多时读响应会抛 java.lang.RuntimeException: index out of range，
        //    表现为数据其实已插入、但任务抛 UncategorizedSQLException（2026-11-12 本地实测 266 条必现）。
        //    这里改为逐条 insert（1 小时才 266 条，成本可忽略），单条失败不影响其余回路。
        int saved = 0;
        int failed = 0;
        for (LightingCircuitCurrentHistory row : rows) {
            try {
                super.save(row);
                saved++;
            } catch (Exception e) {
                failed++;
                if (failed <= 3) {
                    log.error("【电流采样】单条写入失败 circuitId={}, code={}",
                            row.getCircuitId(), row.getCircuitCode(), e);
                }
            }
        }
        log.info("【电流采样】collectTime={}，回路总数={}，应写={}，成功={}，失败={}，电流为空跳过={}",
                collectTime, unique.size(), rows.size(), saved, failed, nullCurrent);
        return saved;
    }

    @Override
    public boolean existsAt(LocalDateTime collectTime) {
        if (collectTime == null) {
            return false;
        }
        return super.count(new LambdaQueryWrapper<LightingCircuitCurrentHistory>()
                .eq(LightingCircuitCurrentHistory::getCollectTime, collectTime)) > 0;
    }

    @Override
    public List<LightingCircuitCurrentHistory> queryHistory(Long circuitId, Long areaId,
                                                            LocalDateTime start, LocalDateTime end,
                                                            Integer limit) {
        int size = limit == null || limit <= 0 ? 500 : Math.min(limit, 5000);
        LambdaQueryWrapper<LightingCircuitCurrentHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(circuitId != null, LightingCircuitCurrentHistory::getCircuitId, circuitId)
                .eq(areaId != null, LightingCircuitCurrentHistory::getAreaId, areaId)
                .ge(start != null, LightingCircuitCurrentHistory::getCollectTime, start)
                .le(end != null, LightingCircuitCurrentHistory::getCollectTime, end)
                .orderByAsc(LightingCircuitCurrentHistory::getCollectTime)
                .orderByAsc(LightingCircuitCurrentHistory::getCircuitId);
        Page<LightingCircuitCurrentHistory> page = super.page(new Page<>(1, size), wrapper);
        List<LightingCircuitCurrentHistory> records = page == null ? null : page.getRecords();
        return records == null ? Collections.emptyList() : records;
    }
}
