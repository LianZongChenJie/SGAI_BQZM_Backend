package org.jeecg.modules.bems.lighting.service.impl;

import lombok.AllArgsConstructor;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingDataInterface;
import org.jeecg.modules.bems.lighting.service.IDataCollectionService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDataInterfaceService;
import org.jeecg.modules.bems.lighting.vo.DataCollectionStatisticsVo;
import org.jeecg.modules.bems.lighting.vo.DataTypeDistributionVo;
import org.jeecg.modules.bems.lighting.vo.RealtimeDataItemVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据汇集服务实现
 */
@Service
@AllArgsConstructor
public class DataCollectionServiceImpl implements IDataCollectionService {

    private final ILightingDataInterfaceService dataInterfaceService;
    private final ILightingCircuitService circuitService;

    @Override
    public DataCollectionStatisticsVo getCollectionStatistics() {
        DataCollectionStatisticsVo vo = new DataCollectionStatisticsVo();
        List<String> times = new ArrayList<>();
        List<Long> realtimeData = new ArrayList<>();
        List<Long> historyData = new ArrayList<>();

        // 生成24小时时间点
        for (int i = 0; i < 24; i++) {
            times.add(String.format("%02d:00", i));
            // 模拟数据：实时数据和历史数据按小时递增
            // 实际项目中应从实时数据表和历史数据表统计
            long base = 50 + i * 20L;
            realtimeData.add(base + (long) (Math.random() * 30));
            historyData.add(base + 20 + (long) (Math.random() * 20));
        }

        vo.setTimes(times);
        vo.setRealtimeData(realtimeData);
        vo.setHistoryData(historyData);
        return vo;
    }

    @Override
    public List<DataTypeDistributionVo> getDataTypeDistribution() {
        List<LightingDataInterface> interfaces = dataInterfaceService.list();
        long total = interfaces.size();
        if (total == 0) {
            total = 1;
        }

        // 按数据类型分组统计
        Map<String, Long> typeCountMap = interfaces.stream()
                .collect(Collectors.groupingBy(
                        LightingDataInterface::getDataType,
                        Collectors.counting()
                ));

        List<DataTypeDistributionVo> result = new ArrayList<>();

        // 实时数据（电流/电压/功率）
        DataTypeDistributionVo realtimeVo = new DataTypeDistributionVo();
        realtimeVo.setTypeName("实时数据");
        realtimeVo.setTypeDesc("电流/电压/功率");
        long realtimeCount = typeCountMap.getOrDefault("实时数据/模拟量", 0L)
                + typeCountMap.getOrDefault("实时数据/开关量", 0L);
        realtimeVo.setCount(realtimeCount);
        realtimeVo.setPercentage(calcPercentage(realtimeCount, total));
        result.add(realtimeVo);

        // 开关量数据（开关状态）
        DataTypeDistributionVo switchVo = new DataTypeDistributionVo();
        switchVo.setTypeName("开关量数据");
        switchVo.setTypeDesc("开关状态");
        long switchCount = typeCountMap.getOrDefault("开关量/模拟量", 0L);
        switchVo.setCount(switchCount);
        switchVo.setPercentage(calcPercentage(switchCount, total));
        result.add(switchVo);

        // 触发类数据（报警/事件）
        DataTypeDistributionVo triggerVo = new DataTypeDistributionVo();
        triggerVo.setTypeName("触发类数据");
        triggerVo.setTypeDesc("报警/事件");
        triggerVo.setCount(0L);
        triggerVo.setPercentage(BigDecimal.ZERO);
        result.add(triggerVo);

        // 视频数据
        DataTypeDistributionVo videoVo = new DataTypeDistributionVo();
        videoVo.setTypeName("视频数据");
        videoVo.setTypeDesc("视频/监控");
        long videoCount = typeCountMap.getOrDefault("视频/监控", 0L);
        videoVo.setCount(videoCount);
        videoVo.setPercentage(calcPercentage(videoCount, total));
        result.add(videoVo);

        return result;
    }

    @Override
    public List<RealtimeDataItemVo> getRealtimeDataList(int limit) {
        List<RealtimeDataItemVo> result = new ArrayList<>();

        // 从照明回路表获取实时数据
        List<LightingCircuit> circuits = circuitService.list();
        if (circuits == null || circuits.isEmpty()) {
            return result;
        }

        // 按最后在线时间倒序
        circuits.sort((a, b) -> {
            if (a.getLastOnlineTime() == null && b.getLastOnlineTime() == null) return 0;
            if (a.getLastOnlineTime() == null) return 1;
            if (b.getLastOnlineTime() == null) return -1;
            return b.getLastOnlineTime().compareTo(a.getLastOnlineTime());
        });

        for (LightingCircuit circuit : circuits) {
            if (result.size() >= limit) break;

            LocalDateTime time = circuit.getLastOnlineTime() != null
                    ? circuit.getLastOnlineTime() : LocalDateTime.now();

            // 功率数据
            if (circuit.getCurrentPower() != null) {
                RealtimeDataItemVo powerVo = new RealtimeDataItemVo();
                powerVo.setTimestamp(time);
                powerVo.setSpaceName(circuit.getAreaName() != null ? circuit.getAreaName() : "");
                powerVo.setDeviceName(circuit.getCircuitName() != null ? circuit.getCircuitName() : "");
                powerVo.setDataItem("功率");
                powerVo.setValue(circuit.getCurrentPower() + " kW");
                powerVo.setType("正常");
                powerVo.setColor("#67C23A");
                result.add(powerVo);
            }

            // 电压数据
            if (circuit.getVoltage() != null) {
                RealtimeDataItemVo voltageVo = new RealtimeDataItemVo();
                voltageVo.setTimestamp(time);
                voltageVo.setSpaceName(circuit.getAreaName() != null ? circuit.getAreaName() : "");
                voltageVo.setDeviceName(circuit.getCircuitName() != null ? circuit.getCircuitName() : "");
                voltageVo.setDataItem("电压");
                voltageVo.setValue(circuit.getVoltage() + " V");
                voltageVo.setType("正常");
                voltageVo.setColor("#67C23A");
                result.add(voltageVo);
            }

            // 开关状态
            if (circuit.getStatus() != null) {
                RealtimeDataItemVo statusVo = new RealtimeDataItemVo();
                statusVo.setTimestamp(time);
                statusVo.setSpaceName(circuit.getAreaName() != null ? circuit.getAreaName() : "");
                statusVo.setDeviceName(circuit.getCircuitName() != null ? circuit.getCircuitName() : "");
                statusVo.setDataItem("开关状态");
                statusVo.setValue(circuit.getStatus());
                statusVo.setType("正常");
                statusVo.setColor("#67C23A");
                result.add(statusVo);
            }
        }

        // 限制返回数量
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }

        return result;
    }

    private BigDecimal calcPercentage(long part, long total) {
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
