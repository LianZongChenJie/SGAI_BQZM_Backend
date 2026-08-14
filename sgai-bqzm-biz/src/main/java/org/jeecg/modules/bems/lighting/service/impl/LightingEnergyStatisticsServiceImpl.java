package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.entity.LightingEnergyHour;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.jeecg.modules.bems.lighting.service.ILightingEnergyHourService;
import org.jeecg.modules.bems.lighting.service.ILightingEnergyStatisticsService;
import org.jeecg.modules.bems.lighting.vo.EnergyProportionVo;
import org.jeecg.modules.bems.lighting.vo.EnergyRankItemVo;
import org.jeecg.modules.bems.lighting.vo.EnergySummaryNodeVo;
import org.jeecg.modules.bems.lighting.vo.EnergyTrendVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 能耗统计（对应原型"能耗统计"页）：
 * - 数据源：lighting_energy_hour（整点任务聚合的小时用电量）
 * - 装机功率：lighting_circuit.rated_power
 * - 电表数：统计范围内有读数的网关数（一个网关=一块电表）
 * - 级别：parcel-按地块、zone-按区域、box-按箱子（箱子=区域内网关）
 * - 未关联到区域的读数（area_id 为空）归入"待确认映射"
 */
@Slf4j
@Service
@AllArgsConstructor
public class LightingEnergyStatisticsServiceImpl implements ILightingEnergyStatisticsService {

    private static final String LEVEL_PARCEL = "parcel";
    private static final String LEVEL_ZONE = "zone";
    private static final String LEVEL_BOX = "box";
    private static final String UNASSIGNED = "待确认映射";

    private final ILightingEnergyHourService energyHourService;
    private final ILightingCircuitService circuitService;
    private final ILightingAreaService areaService;
    private final ILightingDistrictService districtService;

    @Override
    public List<EnergyRankItemVo> ranking(String level, String date, Integer top) {
        LocalDate day = parseDate(date);
        String lv = normalizeLevel(level);
        List<EnergyAgg> aggs = aggregate(lv, day);
        aggs.sort((a, b) -> b.getToday().compareTo(a.getToday()));
        BigDecimal grandToday = aggs.stream().map(EnergyAgg::getToday).reduce(BigDecimal.ZERO, BigDecimal::add);
        int limit = top == null || top <= 0 ? 15 : top;
        return aggs.stream().limit(limit).map(a -> toVo(a, grandToday)).collect(Collectors.toList());
    }

    @Override
    public List<EnergyProportionVo> proportion(String level, String date) {
        LocalDate day = parseDate(date);
        String lv = normalizeLevel(level);
        List<EnergyAgg> aggs = aggregate(lv, day);
        aggs.sort((a, b) -> b.getToday().compareTo(a.getToday()));
        BigDecimal grandToday = aggs.stream().map(EnergyAgg::getToday).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<EnergyProportionVo> out = new ArrayList<>();
        BigDecimal rest = BigDecimal.ZERO;
        for (int i = 0; i < aggs.size(); i++) {
            EnergyAgg a = aggs.get(i);
            if (i < 5) {
                EnergyProportionVo vo = new EnergyProportionVo();
                vo.setName(a.getName());
                vo.setValue(a.getToday().setScale(1, RoundingMode.HALF_UP));
                vo.setRatio(ratio(a.getToday(), grandToday));
                out.add(vo);
            } else {
                rest = rest.add(a.getToday());
            }
        }
        if (rest.signum() > 0) {
            EnergyProportionVo vo = new EnergyProportionVo();
            vo.setName("其他");
            vo.setValue(rest.setScale(1, RoundingMode.HALF_UP));
            vo.setRatio(ratio(rest, grandToday));
            out.add(vo);
        }
        return out;
    }

    @Override
    public EnergyTrendVo hourlyTrend(String level, String date) {
        LocalDate day = parseDate(date);
        String dayStr = day.toString();
        String lv = normalizeLevel(level);
        EnergyTrendVo vo = new EnergyTrendVo();
        vo.setHours(IntStream.range(0, 24).mapToObj(i -> String.format("%02d:00", i)).collect(Collectors.toList()));
        List<LightingEnergyHour> rows = energyHourService.list(new LambdaQueryWrapper<LightingEnergyHour>()
                .eq(LightingEnergyHour::getStatDate, dayStr));
        vo.setSeries(new ArrayList<>());
        if (rows.isEmpty()) {
            return vo;
        }
        Ctx ctx = buildCtx(rows);
        if (LEVEL_PARCEL.equals(lv)) {
            // 按地块：今日 Top5 地块逐时对比
            List<EnergyAgg> aggs = aggregate(LEVEL_PARCEL, day, ctx, rows);
            aggs.sort((a, b) -> b.getToday().compareTo(a.getToday()));
            for (EnergyAgg agg : aggs.stream().limit(5).collect(Collectors.toList())) {
                BigDecimal[] points = new BigDecimal[24];
                Arrays.fill(points, BigDecimal.ZERO);
                for (LightingEnergyHour row : rows) {
                    if (row.getStatHour() == null || row.getStatHour() < 0 || row.getStatHour() > 23) {
                        continue;
                    }
                    if (!belongsToParcel(row, agg, ctx)) {
                        continue;
                    }
                    points[row.getStatHour()] = points[row.getStatHour()]
                            .add(row.getEnergy() == null ? BigDecimal.ZERO : row.getEnergy());
                }
                EnergyTrendVo.Series s = new EnergyTrendVo.Series();
                s.setName(agg.getName());
                s.setPoints(Arrays.stream(points).map(p -> p.setScale(1, RoundingMode.HALF_UP)).collect(Collectors.toList()));
                vo.getSeries().add(s);
            }
        } else {
            // 其他级别：全园单序列
            BigDecimal[] points = new BigDecimal[24];
            Arrays.fill(points, BigDecimal.ZERO);
            for (LightingEnergyHour row : rows) {
                if (row.getStatHour() == null || row.getStatHour() < 0 || row.getStatHour() > 23) {
                    continue;
                }
                points[row.getStatHour()] = points[row.getStatHour()]
                        .add(row.getEnergy() == null ? BigDecimal.ZERO : row.getEnergy());
            }
            EnergyTrendVo.Series s = new EnergyTrendVo.Series();
            s.setName("全园");
            s.setPoints(Arrays.stream(points).map(p -> p.setScale(1, RoundingMode.HALF_UP)).collect(Collectors.toList()));
            vo.getSeries().add(s);
        }
        return vo;
    }

    @Override
    public List<EnergySummaryNodeVo> summary(String date) {
        LocalDate day = parseDate(date);
        String dayStr = day.toString();
        List<LightingEnergyHour> rows = energyHourService.list(new LambdaQueryWrapper<LightingEnergyHour>()
                .ge(LightingEnergyHour::getStatDate, day.withDayOfMonth(1).toString())
                .le(LightingEnergyHour::getStatDate, dayStr));
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Ctx ctx = buildCtx(rows);
        List<EnergyAgg> parcels = aggregate(LEVEL_PARCEL, day, ctx, rows);
        List<EnergyAgg> zones = aggregate(LEVEL_ZONE, day, ctx, rows);
        List<EnergyAgg> boxes = aggregate(LEVEL_BOX, day, ctx, rows);
        BigDecimal grandToday = parcels.stream().map(EnergyAgg::getToday).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 区域挂到地块下
        Map<String, List<EnergyAgg>> zoneByParcel = new HashMap<>();
        for (EnergyAgg z : zones) {
            String pk = z.isUnassigned() ? "P|" + UNASSIGNED : "P|" + z.getDistrictId();
            zoneByParcel.computeIfAbsent(pk, k -> new ArrayList<>()).add(z);
        }
        // 箱子挂到区域下
        Map<String, List<EnergyAgg>> boxByZone = new HashMap<>();
        for (EnergyAgg b : boxes) {
            String zk = b.isUnassigned() ? "Z|U|" + b.getGateway() : "Z|" + b.getAreaId();
            boxByZone.computeIfAbsent(zk, k -> new ArrayList<>()).add(b);
        }

        parcels.sort((a, b) -> b.getToday().compareTo(a.getToday()));
        List<EnergySummaryNodeVo> result = new ArrayList<>();
        for (EnergyAgg p : parcels) {
            EnergySummaryNodeVo pn = toNode(p, grandToday);
            List<EnergySummaryNodeVo> zns = new ArrayList<>();
            for (EnergyAgg z : zoneByParcel.getOrDefault(p.getKey(), Collections.emptyList())) {
                EnergySummaryNodeVo zn = toNode(z, grandToday);
                List<EnergySummaryNodeVo> bns = boxByZone.getOrDefault(z.getKey(), Collections.emptyList()).stream()
                        .map(b -> toNode(b, grandToday))
                        .collect(Collectors.toList());
                bns.sort((x, y) -> y.getToday().compareTo(x.getToday()));
                zn.setChildren(bns);
                zns.add(zn);
            }
            zns.sort((x, y) -> y.getToday().compareTo(x.getToday()));
            pn.setChildren(zns);
            result.add(pn);
        }
        return result;
    }

    // ==================== 内部实现 ====================

    private String normalizeLevel(String level) {
        if (level == null) {
            return LEVEL_PARCEL;
        }
        switch (level.trim()) {
            case "zone":
            case "区域":
                return LEVEL_ZONE;
            case "box":
            case "箱子":
                return LEVEL_BOX;
            default:
                return LEVEL_PARCEL;
        }
    }

    private LocalDate parseDate(String date) {
        if (StringUtils.isBlank(date)) {
            return LocalDate.now();
        }
        String d = date.trim();
        try {
            if (d.contains("-")) {
                return LocalDate.parse(d);
            }
            return LocalDate.parse(d, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    /**
     * 加载指定月份区间内的小时能耗（数据源）
     */
    private List<LightingEnergyHour> loadRows(LocalDate day) {
        return energyHourService.list(new LambdaQueryWrapper<LightingEnergyHour>()
                .ge(LightingEnergyHour::getStatDate, day.withDayOfMonth(1).toString())
                .le(LightingEnergyHour::getStatDate, day.toString()));
    }

    /**
     * 按级别聚合
     */
    private List<EnergyAgg> aggregate(String level, LocalDate day) {
        List<LightingEnergyHour> rows = loadRows(day);
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }
        return aggregate(level, day, buildCtx(rows), rows);
    }

    private List<EnergyAgg> aggregate(String level, LocalDate day, Ctx ctx, List<LightingEnergyHour> rows) {
        String dayStr = day.toString();
        Map<String, EnergyAgg> map = new LinkedHashMap<>();
        for (LightingEnergyHour row : rows) {
            Long areaId = row.getAreaId();
            String gateway = row.getGatewayCode();
            boolean isToday = dayStr.equals(row.getStatDate());
            EnergyAgg agg;
            if (LEVEL_PARCEL.equals(level)) {
                if (areaId == null) {
                    agg = map.computeIfAbsent("P|" + UNASSIGNED, k -> newAgg(k, true, null, null, null));
                } else {
                    Long districtId = ctx.areaMap.containsKey(areaId) ? ctx.areaMap.get(areaId).getDistrictId() : null;
                    String key = "P|" + (districtId == null ? "null" : districtId);
                    agg = map.computeIfAbsent(key, k -> newAgg(k, false, districtId, null, null));
                }
            } else if (LEVEL_ZONE.equals(level)) {
                if (areaId == null) {
                    String g = gateway == null ? "未知" : gateway;
                    agg = map.computeIfAbsent("Z|U|" + g, k -> newAgg(k, true, null, null, g));
                } else {
                    String key = "Z|" + areaId;
                    agg = map.computeIfAbsent(key, k -> newAgg(k, false, null, areaId, null));
                }
            } else {
                String g = gateway == null ? "未知" : gateway;
                if (areaId == null) {
                    agg = map.computeIfAbsent("B|U|" + g, k -> newAgg(k, true, null, null, g));
                } else {
                    String key = "B|" + areaId + "|" + g;
                    agg = map.computeIfAbsent(key, k -> newAgg(k, false, null, areaId, g));
                }
            }
            agg.setToday(agg.getToday().add(isToday && row.getEnergy() != null ? row.getEnergy() : BigDecimal.ZERO));
            agg.setMonth(agg.getMonth().add(row.getEnergy() == null ? BigDecimal.ZERO : row.getEnergy()));
            agg.getAreaIds().add(areaId);
            if (gateway != null) {
                agg.getGateways().add(gateway);
            }
        }
        List<EnergyAgg> result = new ArrayList<>(map.values());
        for (EnergyAgg agg : result) {
            fillNameAndKw(agg, level, ctx);
        }
        return result;
    }

    private EnergyAgg newAgg(String key, boolean unassigned, Long districtId, Long areaId, String gateway) {
        EnergyAgg agg = new EnergyAgg();
        agg.setKey(key);
        agg.setUnassigned(unassigned);
        agg.setDistrictId(districtId);
        agg.setAreaId(areaId);
        agg.setGateway(gateway);
        return agg;
    }

    /**
     * 回填名称与装机功率
     */
    private void fillNameAndKw(EnergyAgg agg, String level, Ctx ctx) {
        if (LEVEL_PARCEL.equals(level)) {
            if (!agg.isUnassigned()) {
                LightingDistrict d = agg.getDistrictId() == null ? null : ctx.districtMap.get(agg.getDistrictId());
                agg.setName(d != null ? d.getDistrictName() : "地块" + agg.getDistrictId());
            } else {
                agg.setName(UNASSIGNED);
            }
            agg.setKw(sumAreaKw(agg, ctx));
        } else if (LEVEL_ZONE.equals(level)) {
            if (!agg.isUnassigned()) {
                LightingArea area = agg.getAreaId() == null ? null : ctx.areaMap.get(agg.getAreaId());
                if (area != null) {
                    LightingDistrict d = area.getDistrictId() == null ? null : ctx.districtMap.get(area.getDistrictId());
                    agg.setDistrictId(area.getDistrictId());
                    agg.setName(d != null ? d.getDistrictName() + " / " + area.getAreaName() : area.getAreaName());
                } else {
                    agg.setName("区域" + agg.getAreaId());
                }
            } else {
                agg.setName(UNASSIGNED + " / " + agg.getGateway());
            }
            agg.setKw(sumAreaKw(agg, ctx));
        } else {
            if (!agg.isUnassigned()) {
                LightingArea area = agg.getAreaId() == null ? null : ctx.areaMap.get(agg.getAreaId());
                String areaName = area != null ? area.getAreaName() : ("区域" + agg.getAreaId());
                agg.setName(areaName + "·" + agg.getGateway() + "号网关");
                agg.setKw(ctx.boxKw.getOrDefault(agg.getAreaId() + "|" + agg.getGateway(), BigDecimal.ZERO));
            } else {
                agg.setName(agg.getGateway() + "号网关");
            }
        }
    }

    private BigDecimal sumAreaKw(EnergyAgg agg, Ctx ctx) {
        BigDecimal kw = BigDecimal.ZERO;
        for (Long aid : agg.getAreaIds()) {
            if (aid == null) {
                continue;
            }
            kw = kw.add(ctx.areaKw.getOrDefault(aid, BigDecimal.ZERO));
        }
        return kw;
    }

    private boolean belongsToParcel(LightingEnergyHour row, EnergyAgg agg, Ctx ctx) {
        Long areaId = row.getAreaId();
        if (agg.isUnassigned()) {
            return areaId == null;
        }
        if (areaId == null) {
            return false;
        }
        LightingArea area = ctx.areaMap.get(areaId);
        Long districtId = area == null ? null : area.getDistrictId();
        return districtId != null && districtId.equals(agg.getDistrictId());
    }

    /**
     * 加载区域/片区/回路信息，预聚合装机功率
     */
    private Ctx buildCtx(List<LightingEnergyHour> rows) {
        Ctx ctx = new Ctx();
        Set<Long> areaIds = rows.stream()
                .map(LightingEnergyHour::getAreaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (areaIds.isEmpty()) {
            return ctx;
        }
        List<LightingArea> areas = areaService.listByIds(areaIds);
        ctx.areaMap = areas.stream()
                .collect(Collectors.toMap(LightingArea::getId, Function.identity(), (a, b) -> a));
        Set<Long> districtIds = areas.stream()
                .map(LightingArea::getDistrictId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!districtIds.isEmpty()) {
            ctx.districtMap = districtService.listByIds(districtIds).stream()
                    .collect(Collectors.toMap(LightingDistrict::getId, Function.identity(), (a, b) -> a));
        }
        List<LightingCircuit> circuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                .in(LightingCircuit::getAreaId, areaIds));
        for (LightingCircuit c : circuits) {
            if (c.getAreaId() == null) {
                continue;
            }
            BigDecimal kw = c.getRatedPower() == null ? BigDecimal.ZERO : BigDecimal.valueOf(c.getRatedPower());
            ctx.areaKw.merge(c.getAreaId(), kw, BigDecimal::add);
            String gateway = parseGateway(c, ctx.areaMap.get(c.getAreaId()));
            if (gateway != null) {
                ctx.boxKw.merge(c.getAreaId() + "|" + gateway, kw, BigDecimal::add);
            }
        }
        return ctx;
    }

    /**
     * 从回路编码解析网关编号（北区 12-1 → 12；904 空间回路无网关前缀，固定 54）
     */
    private String parseGateway(LightingCircuit circuit, LightingArea area) {
        String code = circuit.getCircuitCode();
        if (StringUtils.isNotEmpty(code)) {
            int dash = code.indexOf('-');
            if (dash > 0) {
                String gw = code.substring(0, dash);
                if (gw.matches("\\d+")) {
                    return gw;
                }
            }
        }
        if (area != null && "904".equals(area.getSpace())) {
            return "54";
        }
        return null;
    }

    private EnergyRankItemVo toVo(EnergyAgg agg, BigDecimal grandToday) {
        EnergyRankItemVo vo = new EnergyRankItemVo();
        vo.setName(agg.getName());
        vo.setMeters(agg.getGateways().size());
        vo.setKw(agg.getKw().setScale(1, RoundingMode.HALF_UP));
        vo.setToday(agg.getToday().setScale(1, RoundingMode.HALF_UP));
        vo.setMonth(agg.getMonth().setScale(1, RoundingMode.HALF_UP));
        vo.setRatio(ratio(agg.getToday(), grandToday));
        return vo;
    }

    private EnergySummaryNodeVo toNode(EnergyAgg agg, BigDecimal grandToday) {
        EnergySummaryNodeVo node = new EnergySummaryNodeVo();
        node.setName(agg.getName());
        node.setMeters(agg.getGateways().size());
        node.setKw(agg.getKw().setScale(1, RoundingMode.HALF_UP));
        node.setToday(agg.getToday().setScale(1, RoundingMode.HALF_UP));
        node.setMonth(agg.getMonth().setScale(1, RoundingMode.HALF_UP));
        node.setRatio(ratio(agg.getToday(), grandToday));
        node.setChildren(Collections.emptyList());
        return node;
    }

    private BigDecimal ratio(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }

    /**
     * 聚合上下文
     */
    @Data
    private static class Ctx {
        private Map<Long, LightingArea> areaMap = new HashMap<>();
        private Map<Long, LightingDistrict> districtMap = new HashMap<>();
        private Map<Long, BigDecimal> areaKw = new HashMap<>();
        private Map<String, BigDecimal> boxKw = new HashMap<>();
    }

    /**
     * 聚合中间对象
     */
    @Data
    private static class EnergyAgg {
        private String key;
        private String name;
        private BigDecimal today = BigDecimal.ZERO;
        private BigDecimal month = BigDecimal.ZERO;
        private BigDecimal kw = BigDecimal.ZERO;
        private final Set<String> gateways = new HashSet<>();
        private final Set<Long> areaIds = new HashSet<>();
        private boolean unassigned;
        private String gateway;
        private Long districtId;
        private Long areaId;
    }
}
