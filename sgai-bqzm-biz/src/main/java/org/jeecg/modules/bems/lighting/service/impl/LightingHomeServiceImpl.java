package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingHomeService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.LightingService;
import org.jeecg.modules.bems.lighting.vo.AreaStatisticsVo;
import org.jeecg.modules.bems.lighting.vo.EnergyStatisticsVo;
import org.jeecg.modules.bems.lighting.vo.OnlineStatisticsVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
    private final LightingService lightingService;
    private final ILightingOperationLogService lightingOperationLogService;

    @Override
    public AreaStatisticsVo getAreaStatistics() {
        AreaStatisticsVo vo = new AreaStatisticsVo();
        List<LightingArea> list = areaService.list();
        // 按空间名称（spaceName）去重统计地块数量，同一地块下的多条区域记录不重复计数
        long total = list.stream()
                .map(LightingArea::getSpaceName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .count();
        // 已覆盖：按空间名称去重后，存在回路数 > 0 的地块数
        long covered = list.stream()
                .filter(area -> area.getCircuitCount() != null && area.getCircuitCount() > 0)
                .map(LightingArea::getSpaceName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .count();

        vo.setTotalCount(total);
        vo.setCoveredCount(covered);
        vo.setCoverageRate(calcPercentage(covered, total));
        vo.setCoverageRate(BigDecimal.valueOf(Long.parseLong("100")));
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
        if (list.isEmpty()) {
            return;
        }
        // 记录一键全开主日志
        LightingOperationLog mainLog = new LightingOperationLog();
        mainLog.setLogType(LightingOperationLog.LOG_TYPE_ONE_KEY);
        mainLog.setParentId(null);
        mainLog.setRelType("一键控制");
        mainLog.setRelId(0L);
        mainLog.setName("一键全开");
        mainLog.setOperationTime(LocalDateTime.now());
        mainLog.setOperationType("一键全开");
        mainLog.setOperatorType(LightingOperationLog.OPERATOR_TYPE_MANUAL);
        // 设置操作人
        String operationBy = "照明计划";
        try {
            org.jeecg.common.system.vo.LoginUser sysUser = (org.jeecg.common.system.vo.LoginUser) org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
            if (sysUser != null) {
                operationBy = sysUser.getUsername();
            }
        } catch (Exception e) {
            // 异步场景中SecurityManager不可用，使用默认用户
        }
        mainLog.setOperationBy(operationBy);
        lightingOperationLogService.save(mainLog);

        // 逐个开启区域，挂在主日志下
        for (LightingArea area : list) {
            try {
                areaService.open(area.getId(), mainLog.getId());
            } catch (Exception e) {
                log.error("一键全开-区域[{}]开启失败", area.getAreaName(), e);
            }
        }
    }

    @Override
    public void closeAll() {
        List<LightingArea> list = areaService.list();
        if (list.isEmpty()) {
            return;
        }
        // 记录一键全关主日志
        LightingOperationLog mainLog = new LightingOperationLog();
        mainLog.setLogType(LightingOperationLog.LOG_TYPE_ONE_KEY);
        mainLog.setParentId(null);
        mainLog.setRelType("一键控制");
        mainLog.setRelId(0L);
        mainLog.setName("一键全关");
        mainLog.setOperationTime(LocalDateTime.now());
        mainLog.setOperationType("一键全关");
        mainLog.setOperatorType(LightingOperationLog.OPERATOR_TYPE_MANUAL);
        // 设置操作人
        String operationBy = "照明计划";
        try {
            org.jeecg.common.system.vo.LoginUser sysUser = (org.jeecg.common.system.vo.LoginUser) org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
            if (sysUser != null) {
                operationBy = sysUser.getUsername();
            }
        } catch (Exception e) {
            // 异步场景中SecurityManager不可用，使用默认用户
        }
        mainLog.setOperationBy(operationBy);
        lightingOperationLogService.save(mainLog);

        // 逐个关闭区域，挂在主日志下
        for (LightingArea area : list) {
            try {
                areaService.close(area.getId(), mainLog.getId());
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
