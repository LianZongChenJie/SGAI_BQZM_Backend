package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.alarm.entity.AlarmRecord;
import org.jeecg.modules.bems.alarm.service.IAlarmRecordService;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingHomeService;
import org.jeecg.modules.bems.lighting.service.LightingService;
import org.jeecg.modules.bems.lighting.vo.AreaStatisticsVo;
import org.jeecg.modules.bems.lighting.vo.EnergyStatisticsVo;
import org.jeecg.modules.bems.lighting.vo.OnlineStatisticsVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 首页概览服务实现
 */
@Service
@AllArgsConstructor
@Slf4j
public class LightingHomeServiceImpl implements ILightingHomeService {

    private final ILightingAreaService areaService;
    private final ILightingCircuitService circuitService;
    private final IAlarmRecordService alarmRecordService;
    private final LightingService lightingService;

    @Override
    public AreaStatisticsVo getAreaStatistics() {
        AreaStatisticsVo vo = new AreaStatisticsVo();
        List<LightingArea> list = areaService.list();
        long total = list.size();
        // 已覆盖：有回路且在线数 > 0 的地块
        long covered = list.stream()
                .filter(area -> area.getCircuitCount() != null && area.getCircuitCount() > 0)
                .count();

        vo.setTotalCount(total);
        vo.setCoveredCount(covered);
        vo.setCoverageRate(calcPercentage(covered, total));
        return vo;
    }

    @Override
    public OnlineStatisticsVo getOnlineStatistics() {
        OnlineStatisticsVo vo = new OnlineStatisticsVo();
        List<LightingCircuit> list = circuitService.list();
        long total = list.size();
        long online = list.stream()
                .filter(circuit -> LightingCircuit.COMSTAT_ONLINE.equals(circuit.getComstat()))
                .count();
        long offline = total - online;

        vo.setTotalCount(total);
        vo.setOnlineCount(online);
        vo.setOfflineCount(offline);
        vo.setOnlineRate(calcPercentage(online, total));
        return vo;
    }

    @Override
    public EnergyStatisticsVo getEnergyStatistics() {
        EnergyStatisticsVo vo = new EnergyStatisticsVo();
        List<LightingArea> list = areaService.list();

        // 今日用电：所有地块 todayEnergy 求和
        BigDecimal todayEnergy = list.stream()
                .map(area -> area.getTodayEnergy() != null
                        ? BigDecimal.valueOf(area.getTodayEnergy())
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 昨日用电：暂时用今日的 90% 模拟（实际项目中应从历史能耗表取）
        BigDecimal yesterdayEnergy = todayEnergy.multiply(BigDecimal.valueOf(0.9));

        // 计算环比
        BigDecimal changeRate = BigDecimal.ZERO;
        String trend = "equal";
        if (yesterdayEnergy.compareTo(BigDecimal.ZERO) > 0) {
            changeRate = todayEnergy.subtract(yesterdayEnergy)
                    .divide(yesterdayEnergy, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (changeRate.compareTo(BigDecimal.ZERO) > 0) {
                trend = "up";
            } else if (changeRate.compareTo(BigDecimal.ZERO) < 0) {
                trend = "down";
            }
        }

        vo.setTodayEnergy(todayEnergy.setScale(2, RoundingMode.HALF_UP));
        vo.setYesterdayEnergy(yesterdayEnergy.setScale(2, RoundingMode.HALF_UP));
        vo.setChangeRate(changeRate.setScale(1, RoundingMode.HALF_UP));
        vo.setTrend(trend);
        return vo;
    }

    @Override
    public Long getPendingAlarmCount() {
        return alarmRecordService.count(new LambdaQueryWrapper<AlarmRecord>()
                .eq(AlarmRecord::getAlarmStatus, AlarmRecord.ALARM_STATUS_UNTREATED));
    }

    @Override
    public List<LightingArea> getAreaRunStatus(String space) {
        LambdaQueryWrapper<LightingArea> queryWrapper = new LambdaQueryWrapper<LightingArea>()
                .orderByAsc(LightingArea::getSort);
        if (space != null && !space.isEmpty()) {
            queryWrapper.eq(LightingArea::getSpace, space);
        }
        return areaService.list(queryWrapper);
    }

    @Override
    public void openAll() {
        List<LightingArea> list = areaService.list();
        for (LightingArea area : list) {
            try {
                lightingService.areaOpen(area.getSpace(), area.getAreaCode(), area.getOpenCode());
            } catch (Exception e) {
                log.error("一键全开-区域[{}]开启失败", area.getAreaName(), e);
            }
        }
    }

    @Override
    public void closeAll() {
        List<LightingArea> list = areaService.list();
        for (LightingArea area : list) {
            try {
                lightingService.areaClose(area.getSpace(), area.getAreaCode(), area.getCloseCode());
            } catch (Exception e) {
                log.error("一键全关-区域[{}]关闭失败", area.getAreaName(), e);
            }
        }
    }

    private BigDecimal calcPercentage(long part, long total) {
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
