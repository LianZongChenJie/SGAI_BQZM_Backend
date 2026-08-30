package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.entity.LightingEnergyHour;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetryHistory;
import org.jeecg.modules.bems.lighting.mapper.LightingBoxTelemetryHistoryMapper;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.jeecg.modules.bems.lighting.service.ILightingEnergyHourService;
import org.jeecg.modules.bems.lighting.service.ILightingEnergyStatisticsService;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;
import org.jeecg.modules.bems.permission.service.RoleDataPermissionService;
import org.jeecg.modules.bems.permission.vo.UserDataScope;
import org.jeecg.modules.bems.lighting.vo.EnergyMeterReadVo;
import org.jeecg.modules.bems.lighting.vo.EnergyProportionVo;
import org.jeecg.modules.bems.lighting.vo.EnergyRankItemVo;
import org.jeecg.modules.bems.lighting.vo.EnergySummaryItemVo;
import org.jeecg.modules.bems.lighting.vo.EnergySummaryNodeVo;
import org.jeecg.modules.bems.lighting.vo.EnergyTrendVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.time.format.DateTimeFormatter;
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
    private final LightingBoxTelemetryHistoryMapper boxHistoryMapper;
    private final RoleDataPermissionService roleDataPermissionService;

    @Override
    public List<EnergyRankItemVo> ranking(String level, String date, Integer top) {
        LocalDate day = parseDate(date);
        String lv = normalizeLevel(level);
        List<EnergyAgg> aggs = filterByPermission(aggregate(lv, day));
        aggs.sort((a, b) -> b.getToday().compareTo(a.getToday()));
        BigDecimal grandToday = aggs.stream().map(EnergyAgg::getToday).reduce(BigDecimal.ZERO, BigDecimal::add);
        int limit = top == null || top <= 0 ? 15 : top;
        return aggs.stream().limit(limit).map(a -> toVo(a, grandToday)).collect(Collectors.toList());
    }

    @Override
    public List<EnergyProportionVo> proportion(String level, String date) {
        LocalDate day = parseDate(date);
        String lv = normalizeLevel(level);
        List<EnergyAgg> aggs = filterByPermission(aggregate(lv, day));
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
            List<EnergyAgg> aggs = filterByPermission(aggregate(LEVEL_PARCEL, day, ctx, rows));
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
        List<EnergyAgg> parcels = filterByPermission(aggregate(LEVEL_PARCEL, day, ctx, rows));
        List<EnergyAgg> zones = filterByPermission(aggregate(LEVEL_ZONE, day, ctx, rows));
        List<EnergyAgg> boxes = filterByPermission(aggregate(LEVEL_BOX, day, ctx, rows));
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

    @Override
    public List<EnergySummaryItemVo> summaryList(String date, Long districtId, String boxName) {
        LocalDate day = parseDate(date);
        String dayStr = day.toString();
        List<LightingEnergyHour> rows = energyHourService.list(new LambdaQueryWrapper<LightingEnergyHour>()
                .ge(LightingEnergyHour::getStatDate, day.withDayOfMonth(1).toString())
                .le(LightingEnergyHour::getStatDate, dayStr));
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Ctx ctx = buildCtx(rows);
        List<EnergyAgg> boxes = filterByPermission(aggregate(LEVEL_BOX, day, ctx, rows));
        BigDecimal grandToday = boxes.stream().map(EnergyAgg::getToday).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 过滤条件：片区按 id 精确过滤，箱子名称按模糊匹配
        String boxCond = StringUtils.isBlank(boxName) ? null : boxName.trim();

        List<EnergySummaryItemVo> result = new ArrayList<>();
        for (EnergyAgg b : boxes) {
            // 区域名称与所属片区信息
            String areaNameResolved;
            Long areaDistrictId = null;
            if (b.getAreaId() != null) {
                LightingArea area = ctx.areaMap.get(b.getAreaId());
                areaNameResolved = area != null ? area.getAreaName() : ("区域" + b.getAreaId());
                areaDistrictId = area != null ? area.getDistrictId() : null;
            } else {
                areaNameResolved = "";
            }
            // 网关编号（箱子名：XX号网关）
            String gatewayCode = b.getGateway() == null ? "未知" : b.getGateway();
            String boxNameResolved = gatewayCode + "号网关";

            // 按查询条件过滤
            if (districtId != null && !districtId.equals(areaDistrictId)) {
                continue;
            }
            if (boxCond != null && !boxNameResolved.contains(boxCond)) {
                continue;
            }

            // 片区名称（与区域名称同时返回，前端按需选用）
            String districtNameResolved;
            if (areaDistrictId != null) {
                LightingDistrict d = ctx.districtMap.get(areaDistrictId);
                districtNameResolved = d != null ? d.getDistrictName() : ("地块" + areaDistrictId);
            } else {
                districtNameResolved = areaNameResolved;
            }

            EnergySummaryItemVo vo = new EnergySummaryItemVo();
            vo.setAreaName(areaNameResolved);
            vo.setDistrictName(districtNameResolved);
            vo.setBoxName(boxNameResolved);
            vo.setGatewayCode(gatewayCode);
            vo.setMeters(b.getGateways().size());
            vo.setKw(b.getKw().setScale(1, RoundingMode.HALF_UP));
            vo.setToday(b.getToday().setScale(1, RoundingMode.HALF_UP));
            vo.setMonth(b.getMonth().setScale(1, RoundingMode.HALF_UP));
            vo.setRatio(ratio(b.getToday(), grandToday));
            result.add(vo);
        }
        // 按今日用电量降序
        result.sort((a, b) -> b.getToday().compareTo(a.getToday()));
        return result;
    }

    @Override
    public List<EnergyMeterReadVo> meterReads(Long districtId, String areaCode, String gateway, String startTime, String endTime) {
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);
        if (start == null && end == null) {
            // 均未给时间时，默认查询今天 0 点至今
            end = LocalDateTime.now();
            start = end.toLocalDate().atStartOfDay();
        }
        if (end == null) {
            end = LocalDateTime.now();
        }
        if (start == null) {
            start = end.minusDays(1);
        }
        if (start.isAfter(end)) {
            LocalDateTime tmp = start;
            start = end;
            end = tmp;
        }

        // 数据权限：bqzm 放行；非 bqzm 时 districtId 限定在权限片区集合内
        Set<Long> allowedDistricts = null;
        if (!isBqzmRole()) {
            allowedDistricts = getDistrictPermissionIds();
            if (CollectionUtil.isEmpty(allowedDistricts)) {
                // 非 bqzm 且未配置任何片区权限：无可访问数据
                return new ArrayList<>();
            }
        }

        // 1. 片区下区域集合（districtId 为空或 <=0 表示查询全部片区）
        List<Long> areaIds = new ArrayList<>();
        Map<Long, LightingArea> areaMap = new HashMap<>();
        if (districtId != null && districtId > 0) {
            if (allowedDistricts != null && !allowedDistricts.contains(districtId)) {
                // 非 bqzm 且请求的片区不在权限内：无权限
                return new ArrayList<>();
            }
            List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                    .eq(LightingArea::getDistrictId, districtId));
            for (LightingArea a : areas) {
                areaIds.add(a.getId());
                areaMap.put(a.getId(), a);
            }
        } else if (allowedDistricts != null) {
            // 非 bqzm 查询全部片区：限定在权限片区集合内
            List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                    .in(LightingArea::getDistrictId, allowedDistricts));
            for (LightingArea a : areas) {
                areaIds.add(a.getId());
                areaMap.put(a.getId(), a);
            }
        }

        // 1.1 按区域编号(areaCode)精确过滤：只查该区域下的箱子
        if (areaCode != null && !areaCode.isEmpty()) {
            List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                    .eq(LightingArea::getAreaCode, areaCode));
            Set<Long> targetIds = new HashSet<>();
            for (LightingArea a : areas) {
                targetIds.add(a.getId());
                areaMap.put(a.getId(), a);
            }
            // 与片区筛选结果取交集；未传片区时直接用该区域
            if (areaIds.isEmpty()) {
                areaIds.addAll(targetIds);
            } else {
                areaIds.retainAll(targetIds);
            }
        }

        // 2. 确定区间内要统计的网关集合
        //    - 若指定了网关：只查该网关
        //    - 否则：查区间内活跃的网关去重集合（按区域过滤，避免加载区间内全部原始记录导致超时）
        Set<String> gateways;
        if (gateway != null && !gateway.isEmpty()) {
            gateways = new HashSet<>();
            gateways.add(gateway);
        } else {
            List<LightingBoxTelemetryHistory> distinctRows =
                    boxHistoryMapper.selectDistinctGateways(start, end, areaIds.isEmpty() ? null : areaIds);
            gateways = new HashSet<>();
            if (distinctRows != null) {
                for (LightingBoxTelemetryHistory row : distinctRows) {
                    if (row.getGatewayCode() != null) {
                        gateways.add(row.getGatewayCode());
                    }
                }
            }
        }
        if (gateways.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 一次性构建"网关号 -> 区域"映射（避免循环内逐网关反查，防止 N+1）
        Map<String, LightingArea> gatewayAreaMap = new HashMap<>();
        List<LightingArea> allAreas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .isNotNull(LightingArea::getAreaCode));
        if (allAreas != null) {
            for (LightingArea a : allAreas) {
                String code = a.getAreaCode();
                if (code == null || code.isEmpty()) {
                    continue;
                }
                int dot = code.lastIndexOf('.');
                String tail = dot >= 0 ? code.substring(dot + 1).trim() : code.trim();
                if (!tail.isEmpty()) {
                    gatewayAreaMap.putIfAbsent(tail, a);
                }
            }
        }

        List<EnergyMeterReadVo> result = new ArrayList<>();
        for (String gw : gateways) {
            EnergyMeterReadVo vo = new EnergyMeterReadVo();
            vo.setGatewayCode(gw);

            // 区域信息（通过网关号查映射；命中后展示区域名/片区名）
            LightingArea area = gatewayAreaMap.get(gw);
            if (area != null) {
                vo.setBoxName(area.getAreaName());
                vo.setAreaName(area.getAreaName());
                vo.setDistrictName(resolveDistrictName(area.getDistrictId()));
            }

            // 开始表底：开始时间前最近一条累计电量
            LightingBoxTelemetryHistory startRead = boxHistoryMapper.selectLastBeforeByGateway(gw, start);
            // 结束表底：结束时间前最近一条累计电量
            LightingBoxTelemetryHistory endRead = boxHistoryMapper.selectLastBeforeByGateway(gw, end);

            if (startRead != null) {
                vo.setStartTime(toLocalDateTime(startRead.getCollectTime()));
                vo.setStartValue(startRead.getTotalEnergy() == null
                        ? BigDecimal.ZERO : BigDecimal.valueOf(startRead.getTotalEnergy()));
            } else {
                vo.setStartTime(start);
                vo.setStartValue(BigDecimal.ZERO);
            }
            if (endRead != null) {
                vo.setEndTime(toLocalDateTime(endRead.getCollectTime()));
                vo.setEndValue(endRead.getTotalEnergy() == null
                        ? BigDecimal.ZERO : BigDecimal.valueOf(endRead.getTotalEnergy()));
            } else {
                vo.setEndTime(end);
                vo.setEndValue(vo.getStartValue());
            }

            vo.setTotal(vo.getEndValue().subtract(vo.getStartValue()));
            if (vo.getTotal().signum() < 0) {
                vo.setTotal(BigDecimal.ZERO);
            }
            result.add(vo);
        }

        // 按累计用电量降序
        result.sort((a, b) -> b.getTotal().compareTo(a.getTotal()));
        return result;
    }

    /** java.util.Date 转 LocalDateTime（History 表的 collect_time 为 Date，Vo 的时间为 LocalDateTime） */
    private LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null
                : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /** 解析片区名称 */
    private String resolveDistrictName(Long districtId) {
        if (districtId == null) {
            return null;
        }
        LightingDistrict d = districtService.getById(districtId);
        return d != null ? d.getDistrictName() : "地块" + districtId;
    }

    /**
     * 按网关号反查区域：匹配 lighting_area.area_code 小数点后末段（如 area_code=10.22.160.11 → 网关号 11）
     * 用于 history 记录 area_id 为空时，仍能通过 gateway_code 补全区域信息
     */
    private LightingArea resolveAreaByGateway(String gatewayCode) {
        if (gatewayCode == null || gatewayCode.isEmpty()) {
            return null;
        }
        List<LightingArea> areas = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .isNotNull(LightingArea::getAreaCode));
        if (areas == null || areas.isEmpty()) {
            return null;
        }
        for (LightingArea a : areas) {
            String code = a.getAreaCode();
            if (code == null || code.isEmpty()) {
                continue;
            }
            int dot = code.lastIndexOf('.');
            String tail = dot >= 0 ? code.substring(dot + 1).trim() : code.trim();
            if (gatewayCode.equals(tail)) {
                return a;
            }
        }
        return null;
    }

    /** 解析时间（yyyy-MM-dd HH:mm:ss），解析失败返回 null */
    private LocalDateTime parseDateTime(String time) {
        if (StringUtils.isBlank(time)) {
            return null;
        }
        String t = time.trim().replace('T', ' ');
        try {
            if (t.contains(" ")) {
                return LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            if (t.length() == 10) {
                return LocalDate.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            }
            return LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 内部实现 ====================

    /**
     * 判断当前登录用户是否拥有 bqzm 角色（与综合预览角色判断保持一致）
     */
    private boolean isBqzmRole() {
        try {
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            if (sysUser == null || StringUtils.isEmpty(sysUser.getRoleCode())) {
                return false;
            }
            for (String roleCode : sysUser.getRoleCode().split(",")) {
                if (StringUtils.trim(roleCode).equals("bqzm")) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("【能耗统计】判断 bqzm 角色失败，按非 bqzm 处理", e);
        }
        return false;
    }

    /**
     * 数据权限过滤：bqzm 角色返回全部；其他角色仅保留其有权限片区下的数据
     * 权限片区集合从 role_data_permission 配置表读取（DISTRICT 类型）
     * - parcel(地块)级别：直接按聚合结果的 districtId 判断
     * - zone/box(区域/箱子)级别：通过区域反查所属片区判断
     * - 未关联区域（待确认映射）的数据，非 bqzm 角色一律不展示
     */
    private List<EnergyAgg> filterByPermission(List<EnergyAgg> aggs) {
        if (isBqzmRole()) {
            return aggs;
        }
        Set<Long> allowedDistricts = getDistrictPermissionIds();
        if (CollectionUtil.isEmpty(allowedDistricts)) {
            // 非 bqzm 且未配置任何片区权限：不展示任何能耗数据
            return new ArrayList<>();
        }
        if (aggs == null || aggs.isEmpty()) {
            return aggs;
        }
        // 收集所有 areaId，反查区域 → 片区
        Set<Long> allAreaIds = aggs.stream()
                .filter(a -> a.getAreaId() != null)
                .map(EnergyAgg::getAreaId)
                .collect(Collectors.toSet());
        Map<Long, Long> areaDistrict = new HashMap<>();
        if (!allAreaIds.isEmpty()) {
            for (LightingArea area : areaService.listByIds(allAreaIds)) {
                if (area.getId() != null) {
                    areaDistrict.put(area.getId(), area.getDistrictId());
                }
            }
        }
        return aggs.stream().filter(a -> {
            if (a.isUnassigned()) {
                return false;
            }
            Long did = a.getDistrictId();
            if (did == null && a.getAreaId() != null) {
                did = areaDistrict.get(a.getAreaId());
            }
            return did != null && allowedDistricts.contains(did);
        }).collect(Collectors.toList());
    }

    /**
     * 从 role_data_permission 配置表读取当前用户有权限的片区(DISTRICT)ID集合
     * 未登录或读取失败返回空集合
     */
    private Set<Long> getDistrictPermissionIds() {
        try {
            UserDataScope scope = roleDataPermissionService.getCurrentUserDataScope();
            Set<Long> ids = scope != null ? scope.getPermissionIds(RoleDataPermission.TYPE_DISTRICT) : null;
            return ids == null ? new HashSet<>() : ids;
        } catch (Exception e) {
            log.warn("【能耗统计】读取片区权限失败，按无权限处理", e);
            return new HashSet<>();
        }
    }

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
