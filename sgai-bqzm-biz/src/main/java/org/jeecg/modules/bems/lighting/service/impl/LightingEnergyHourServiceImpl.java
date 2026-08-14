package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingEnergyHour;
import org.jeecg.modules.bems.lighting.entity.LightingEnergyRead;
import org.jeecg.modules.bems.lighting.mapper.LightingEnergyHourMapper;
import org.jeecg.modules.bems.lighting.mapper.LightingEnergyReadMapper;
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

    private final LightingEnergyReadMapper energyReadMapper;

    @Override
    @Transactional
    public void aggregateHour(LocalDateTime hourStart) {
        LocalDateTime hourEnd = hourStart.plusHours(1);
        // 该小时内有读数的表计（网关+回路）去重集合
        List<LightingEnergyRead> meters = energyReadMapper.selectDistinctMeters(hourStart, hourEnd);
        if (CollectionUtil.isEmpty(meters)) {
            log.debug("【能耗统计】小时 {} 无电量读数，跳过", hourStart);
            return;
        }
        String statDate = hourStart.toLocalDate().toString();
        int statHour = hourStart.getHour();
        int saved = 0;
        for (LightingEnergyRead meter : meters) {
            try {
                String gateway = meter.getGatewayCode();
                String circuit = meter.getCircuitCode();
                // 该小时末条累计读数
                LightingEnergyRead last = energyReadMapper.selectLastBefore(gateway, circuit, hourEnd);
                if (last == null || last.getValue() == null) {
                    continue;
                }
                // 基准值：上小时末条累计读数；没有则用本小时最早一条近似
                LightingEnergyRead prev = energyReadMapper.selectLastBefore(gateway, circuit, hourStart);
                BigDecimal base;
                if (prev != null && prev.getValue() != null) {
                    base = prev.getValue();
                } else {
                    LightingEnergyRead first = energyReadMapper.selectFirstInRange(gateway, circuit, hourStart, hourEnd);
                    base = first != null && first.getValue() != null ? first.getValue() : last.getValue();
                }
                BigDecimal energy = last.getValue().subtract(base);
                if (energy.signum() < 0) {
                    energy = BigDecimal.ZERO;
                }
                if (energy.signum() == 0) {
                    // 该小时无用电，不落库
                    continue;
                }
                // 幂等：同表计同小时先删旧记录再插入
                remove(new LambdaQueryWrapper<LightingEnergyHour>()
                        .eq(LightingEnergyHour::getStatDate, statDate)
                        .eq(LightingEnergyHour::getStatHour, statHour)
                        .eq(LightingEnergyHour::getGatewayCode, gateway)
                        .eq(LightingEnergyHour::getCircuitCode, circuit));
                LightingEnergyHour hour = new LightingEnergyHour();
                hour.setStatDate(statDate);
                hour.setStatHour(statHour);
                hour.setGatewayCode(gateway);
                hour.setCircuitCode(circuit);
                hour.setAreaId(last.getAreaId());
                hour.setAreaCode(last.getAreaCode());
                hour.setSpace(last.getSpace());
                hour.setEnergy(energy);
                hour.setCreateTime(LocalDateTime.now());
                save(hour);
                saved++;
            } catch (Exception e) {
                log.error("【能耗统计】小时聚合异常：hour={}, gateway={}, circuit={}",
                        hourStart, meter.getGatewayCode(), meter.getCircuitCode(), e);
            }
        }
        log.info("【能耗统计】小时电量统计完成：{}（{}:00），表计 {} 个，入库 {} 条",
                statDate, statHour, meters.size(), saved);
    }
}
