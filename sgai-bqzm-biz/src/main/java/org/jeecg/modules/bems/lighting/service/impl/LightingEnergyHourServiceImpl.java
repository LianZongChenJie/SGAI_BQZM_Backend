package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetryHistory;
import org.jeecg.modules.bems.lighting.entity.LightingEnergyHour;
import org.jeecg.modules.bems.lighting.mapper.LightingBoxTelemetryHistoryMapper;
import org.jeecg.modules.bems.lighting.mapper.LightingEnergyHourMapper;
import org.jeecg.modules.bems.lighting.service.ILightingEnergyHourService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class LightingEnergyHourServiceImpl extends ServiceImpl<LightingEnergyHourMapper, LightingEnergyHour>
        implements ILightingEnergyHourService {

    /**
     * 箱子遥测历史表（含累计电量 total_energy），作为小时聚合的数据源
     */
    private final LightingBoxTelemetryHistoryMapper boxHistoryMapper;

    @Override
    @Transactional
    public void aggregateHour(LocalDateTime hourStart) {
        LocalDateTime hourEnd = hourStart.plusHours(1);
        // 该小时内有累计电量的箱子（网关）去重集合
        List<LightingBoxTelemetryHistory> gateways = boxHistoryMapper.selectDistinctGateways(hourStart, hourEnd);
        if (CollectionUtil.isEmpty(gateways)) {
            log.debug("【能耗统计】小时 {} 无箱子遥测累计电量，跳过", hourStart);
            return;
        }
        String statDate = hourStart.toLocalDate().toString();
        int statHour = hourStart.getHour();
        int saved = 0;
        for (LightingBoxTelemetryHistory gatewayRow : gateways) {
            try {
                String gateway = gatewayRow.getGatewayCode();
                // 该小时末条累计读数（totalEnergy 为 Double，转 BigDecimal）
                LightingBoxTelemetryHistory last = boxHistoryMapper.selectLastBeforeByGateway(gateway, hourEnd);
                if (last == null || last.getTotalEnergy() == null) {
                    continue;
                }
                BigDecimal lastTotal = BigDecimal.valueOf(last.getTotalEnergy());
                // 基准值：上小时末条累计读数；没有则用本小时最早一条近似
                LightingBoxTelemetryHistory prev = boxHistoryMapper.selectLastBeforeByGateway(gateway, hourStart);
                BigDecimal base;
                if (prev != null && prev.getTotalEnergy() != null) {
                    base = BigDecimal.valueOf(prev.getTotalEnergy());
                } else {
                    LightingBoxTelemetryHistory first = boxHistoryMapper.selectFirstInRangeByGateway(gateway, hourStart, hourEnd);
                    base = first != null && first.getTotalEnergy() != null
                            ? BigDecimal.valueOf(first.getTotalEnergy()) : lastTotal;
                }
                BigDecimal energy = lastTotal.subtract(base);
                if (energy.signum() < 0) {
                    energy = BigDecimal.ZERO;
                }
                if (energy.signum() == 0) {
                    // 该小时无用电，不落库
                    continue;
                }
                // 幂等：同网关同小时先删旧记录再插入
                remove(new LambdaQueryWrapper<LightingEnergyHour>()
                        .eq(LightingEnergyHour::getStatDate, statDate)
                        .eq(LightingEnergyHour::getStatHour, statHour)
                        .eq(LightingEnergyHour::getGatewayCode, gateway));
                LightingEnergyHour hour = new LightingEnergyHour();
                hour.setStatDate(statDate);
                hour.setStatHour(statHour);
                hour.setGatewayCode(gateway);
                hour.setCircuitCode(null);
                hour.setAreaId(last.getAreaId());
                hour.setAreaCode(last.getAreaName());
                hour.setEnergy(energy);
                hour.setCreateTime(LocalDateTime.now());
                save(hour);
                saved++;
            } catch (Exception e) {
                log.error("【能耗统计】小时聚合异常：hour={}, gateway={}",
                        hourStart, gatewayRow.getGatewayCode(), e);
            }
        }
        log.info("【能耗统计】小时电量统计完成：{}（{}:00），箱子 {} 个，入库 {} 条",
                statDate, statHour, gateways.size(), saved);
    }
}
