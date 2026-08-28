package org.jeecg.modules.bems.lighting.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetry;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetryHistory;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.mapper.LightingBoxTelemetryHistoryMapper;
import org.jeecg.modules.bems.lighting.mapper.LightingBoxTelemetryMapper;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingBoxTelemetryService;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 箱子遥测服务实现
 * 数据源：MQ 消息 DataType=7
 */
@Slf4j
@Service
@AllArgsConstructor
public class LightingBoxTelemetryServiceImpl extends ServiceImpl<LightingBoxTelemetryMapper, LightingBoxTelemetry>
        implements ILightingBoxTelemetryService {

    private final LightingBoxTelemetryMapper boxMapper;
    private final LightingBoxTelemetryHistoryMapper historyMapper;
    private final ILightingAreaService areaService;
    private final ILightingDistrictService districtService;

    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTelemetry(JSONObject msg) {
        String gatewayCode = msg.getString("GatewayCode");
        if (gatewayCode == null || gatewayCode.isEmpty()) {
            log.warn("【箱子遥测】GatewayCode为空，跳过: {}", msg.toJSONString());
            return;
        }
        Date collectTime = parseTime(msg.getString("CollectTime"));
        Long areaId = msg.getLong("AreaID");
        // 区域信息：区域名 + 所属片区id（通过 AreaID 反查 lighting_area）
        AreaInfo areaInfo = resolveAreaInfo(msg.getString("AreaName"), areaId);

        LightingBoxTelemetry snap = fillSnapshot(msg, gatewayCode, areaId, areaInfo, collectTime);
        LightingBoxTelemetryHistory hist = fillHistory(msg, gatewayCode, areaId, areaInfo, collectTime);

        // 1. 更新快照表（按 gateway_code UPSERT）
        try {
            boxMapper.delete(new LambdaQueryWrapper<LightingBoxTelemetry>().eq(LightingBoxTelemetry::getGatewayCode, gatewayCode));
            boxMapper.insert(snap);
        } catch (Exception e) {
            log.error("【箱子遥测】更新快照失败 gatewayCode={}", gatewayCode, e);
        }
        // 2. 插历史表（每次一条）
        try {
            historyMapper.insert(hist);
        } catch (Exception e) {
            log.error("【箱子遥测】插历史失败 gatewayCode={}", gatewayCode, e);
        }
    }

    @Override
    public List<LightingBoxTelemetry> listBoxes() {
        List<LightingBoxTelemetry> list = boxMapper.selectList(new LambdaQueryWrapper<LightingBoxTelemetry>()
                .orderByDesc(LightingBoxTelemetry::getCollectTime));
        // 填充片区名；失败不影响基础数据返回（降级）
        try {
            fillDistrictName(list);
        } catch (Exception e) {
            log.error("【箱子遥测】填充片区名失败，降级返回基础数据", e);
        }
        return list;
    }

    /** 批量填充片区名称（表存 district_id，这里批量查片区表回填 districtName，不冗余存片区名） */
    private void fillDistrictName(List<LightingBoxTelemetry> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 1. 收集需要回填区域名的 area_id -> 区域名（仅当 area_name 为空时）
        Set<Long> areaIds = list.stream()
                .filter(b -> b.getAreaId() != null
                        && (b.getAreaName() == null || b.getAreaName().isEmpty()))
                .map(LightingBoxTelemetry::getAreaId)
                .collect(Collectors.toSet());
        Map<Long, String> areaNameMap = new HashMap<>();
        if (!areaIds.isEmpty()) {
            for (LightingArea a : areaService.listByIds(areaIds)) {
                areaNameMap.put(a.getId(), a.getAreaName());
            }
        }
        // 2. 收集 district_id -> 片区名（从实体存的 district_id 取）
        Set<Long> districtIds = list.stream()
                .map(LightingBoxTelemetry::getDistrictId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> districtNameMap = new HashMap<>();
        if (!districtIds.isEmpty()) {
            for (LightingDistrict d : districtService.listByIds(districtIds)) {
                districtNameMap.put(d.getId(), d.getDistrictName());
            }
        }
        // 3. 填充片区名 + 回填区域名
        for (LightingBoxTelemetry box : list) {
            if (box.getDistrictId() != null) {
                box.setDistrictName(districtNameMap.get(box.getDistrictId()));
            }
            if (box.getAreaId() != null && (box.getAreaName() == null || box.getAreaName().isEmpty())) {
                box.setAreaName(areaNameMap.get(box.getAreaId()));
            }
        }
    }

    @Override
    public List<LightingBoxTelemetryHistory> history(String gatewayCode, Date start, Date end) {
        LambdaQueryWrapper<LightingBoxTelemetryHistory> qw = new LambdaQueryWrapper<>();
        qw.eq(LightingBoxTelemetryHistory::getGatewayCode, gatewayCode);
        if (start != null) qw.ge(LightingBoxTelemetryHistory::getCollectTime, start);
        if (end != null) qw.le(LightingBoxTelemetryHistory::getCollectTime, end);
        qw.orderByAsc(LightingBoxTelemetryHistory::getCollectTime);
        return historyMapper.selectList(qw);
    }

    /** 区域信息：区域名 + 所属片区id */
    static class AreaInfo {
        String areaName;
        Long districtId;
        AreaInfo(String areaName, Long districtId) { this.areaName = areaName; this.districtId = districtId; }
    }

    /** 填充快照实体 */
    private LightingBoxTelemetry fillSnapshot(JSONObject msg, String gatewayCode, Long areaId, AreaInfo info, Date collectTime) {
        LightingBoxTelemetry t = new LightingBoxTelemetry();
        t.setGatewayCode(gatewayCode);
        t.setAreaId(areaId);
        t.setAreaName(info.areaName);
        t.setDistrictId(info.districtId);
        t.setVoltageA(getDouble(msg, "VoltageA"));
        t.setVoltageB(getDouble(msg, "VoltageB"));
        t.setVoltageC(getDouble(msg, "VoltageC"));
        t.setCurrentA(getDouble(msg, "CurrentA"));
        t.setCurrentB(getDouble(msg, "CurrentB"));
        t.setCurrentC(getDouble(msg, "CurrentC"));
        t.setActivePower(getDouble(msg, "ActivePower"));
        t.setReactivePower(getDouble(msg, "ReactivePower"));
        t.setApparentPower(getDouble(msg, "ApparentPower"));
        t.setPowerFactor(getDouble(msg, "PowerFactor"));
        t.setTotalEnergy(getDouble(msg, "TotalEnergy"));
        t.setCollectTime(collectTime);
        Date now = new Date();
        t.setUpdateTime(now);
        t.setCreateTime(now);
        return t;
    }

    /** 填充历史实体 */
    private LightingBoxTelemetryHistory fillHistory(JSONObject msg, String gatewayCode, Long areaId, AreaInfo info, Date collectTime) {
        LightingBoxTelemetryHistory h = new LightingBoxTelemetryHistory();
        h.setGatewayCode(gatewayCode);
        h.setAreaId(areaId);
        h.setAreaName(info.areaName);
        h.setDistrictId(info.districtId);
        h.setVoltageA(getDouble(msg, "VoltageA"));
        h.setVoltageB(getDouble(msg, "VoltageB"));
        h.setVoltageC(getDouble(msg, "VoltageC"));
        h.setCurrentA(getDouble(msg, "CurrentA"));
        h.setCurrentB(getDouble(msg, "CurrentB"));
        h.setCurrentC(getDouble(msg, "CurrentC"));
        h.setActivePower(getDouble(msg, "ActivePower"));
        h.setReactivePower(getDouble(msg, "ReactivePower"));
        h.setApparentPower(getDouble(msg, "ApparentPower"));
        h.setPowerFactor(getDouble(msg, "PowerFactor"));
        h.setTotalEnergy(getDouble(msg, "TotalEnergy"));
        h.setCollectTime(collectTime);
        h.setCreateTime(new Date());
        return h;
    }

    /** 解析区域信息：区域名 + 所属片区id（通过 AreaID 反查 lighting_area） */
    private AreaInfo resolveAreaInfo(String msgAreaName, Long areaId) {
        // 消息里带了区域名则用，但片区id仍须反查
        if (areaId != null) {
            try {
                LightingArea area = areaService.getById(areaId);
                if (area != null) {
                    String name = (msgAreaName != null && !msgAreaName.isEmpty())
                            ? msgAreaName : area.getAreaName();
                    return new AreaInfo(name, area.getDistrictId());
                }
            } catch (Exception e) {
                log.warn("【箱子遥测】反查区域信息失败 areaId={}", areaId, e);
            }
        }
        return new AreaInfo(
                (msgAreaName != null && !msgAreaName.isEmpty()) ? msgAreaName : null,
                null);
    }

    private Double getDouble(JSONObject msg, String key) {
        Object v = msg.get(key);
        if (v == null) return null;
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private Date parseTime(String s) {
        if (s == null || s.isEmpty()) return new Date();
        try {
            // 兼容 yyyy-MM-dd HH:mm:ss 和 yyyy-MM-ddTHH:mm:ss
            String norm = s.replace('T', ' ');
            if (norm.length() > 19) norm = norm.substring(0, 19);
            return DF.parse(norm);
        } catch (ParseException e) {
            return new Date();
        }
    }
}