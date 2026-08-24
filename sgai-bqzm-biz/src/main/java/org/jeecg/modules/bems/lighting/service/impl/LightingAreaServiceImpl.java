package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.jeecgframework.poi.excel.ExcelExportUtil;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.enmus.ExcelType;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.lighting.dto.LightingAreaExportDto;
import org.jeecg.modules.bems.lighting.dto.LightingAreaQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.mapper.LightingAreaMapper;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.LightingService;
import org.jeecg.modules.bems.lighting.mq.constant.LightingMqConstant;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Service
@Slf4j
public class LightingAreaServiceImpl extends ServiceImpl<LightingAreaMapper, LightingArea> implements ILightingAreaService {

    private final LightingService service;

    private final ILightingOperationLogService lightingOperationLogService;

    private final ILightingCircuitService circuitService;

    private final LightingSendService sendService;

    private final ILightingDistrictService districtService;

    /**
     * 注意：circuitService 与 LightingCircuitServiceImpl.areaService 存在循环依赖，
     * 必须在构造参数上用 @Lazy 打破（Lombok 的 @AllArgsConstructor 不会把字段上的 @Lazy 复制到构造参数上）
     */
    @Autowired
    public LightingAreaServiceImpl(LightingService service,
                                   ILightingOperationLogService lightingOperationLogService,
                                   @Lazy ILightingCircuitService circuitService,
                                   LightingSendService sendService,
                                   ILightingDistrictService districtService) {
        this.service = service;
        this.lightingOperationLogService = lightingOperationLogService;
        this.circuitService = circuitService;
        this.sendService = sendService;
        this.districtService = districtService;
    }

    @Override
    public IPage<LightingArea> listPage(LightingAreaQueryDto params) {
        IPage<LightingArea> page = listPage1(params);
        if(CollectionUtil.isEmpty(page.getRecords())){
            return page;
        }
        List<LightingArea> records = page.getRecords();
        for(LightingArea area : records){
            area.setAreaName(area.getSpaceName() + area.getAreaName());
        }
        return page;
    }

    @Override
    public IPage<LightingArea> listPage1(LightingAreaQueryDto params) {
        LambdaQueryWrapper<LightingArea> queryWrapper = buildQueryWrapper(params);
        IPage<LightingArea> page = super.page(new Page<>(params.getPageNo(), params.getPageSize()),queryWrapper);
        fillDistrictName(page.getRecords());
        // 区域状态按区域下回路的实际状态计算（任一回路开启则区域为开启）
        fillStatusByCircuit(page.getRecords());
        // 区域通讯状态按区域下回路的通讯状态计算（任一回路在线则区域在线）
        fillComstatByCircuit(page.getRecords());
        // 查询MQ中未被消费的下发消息数，更新待下发消息数字段
        fillPendingMsgCount(page.getRecords());
        return page;
    }

    @Override
    public List<LightingArea> list() {
        List<LightingArea> list = super.list();
        fillDistrictName(list);
        return list;
    }

    @Override
    public List<LightingArea> list(Wrapper<LightingArea> queryWrapper) {
        List<LightingArea> list = super.list(queryWrapper);
        fillDistrictName(list);
        return list;
    }

    @Override
    public LightingArea getById(java.io.Serializable id) {
        LightingArea area = super.getById(id);
        if(area != null){
            fillDistrictName(Collections.singletonList(area));
        }
        return area;
    }

    @Override
    @Transactional
    public boolean save(LightingArea entity) {
        boolean ok = super.save(entity);
        // 区域归属片区后，同步刷新片区的 spaceIds
        if (ok && entity.getDistrictId() != null) {
            refreshDistrictSpaceIds(entity.getDistrictId());
        }
        return ok;
    }

    @Override
    @Transactional
    public boolean updateById(LightingArea entity) {
        LightingArea old = entity.getId() == null ? null : super.getById(entity.getId());
        boolean ok = super.updateById(entity);
        if (ok) {
            // 片区归属变化或空间编码变化都会影响片区 spaceIds（按区域实际 space 推导）
            boolean districtChanged = old != null && old.getDistrictId() != null
                    && !old.getDistrictId().equals(entity.getDistrictId());
            boolean spaceChanged = old != null && !Objects.equals(old.getSpace(), entity.getSpace());
            if (districtChanged) {
                // 旧片区刷新
                refreshDistrictSpaceIds(old.getDistrictId());
            }
            if (entity.getDistrictId() != null && (districtChanged || spaceChanged)) {
                // 新片区/所属片区刷新
                refreshDistrictSpaceIds(entity.getDistrictId());
            }
        }
        return ok;
    }

    @Override
    @Transactional
    public boolean removeById(java.io.Serializable id) {
        LightingArea old = super.getById(id);
        boolean ok = super.removeById(id);
        // 删除区域后，同步刷新其原所属片区的 spaceIds
        if (ok && old != null && old.getDistrictId() != null) {
            refreshDistrictSpaceIds(old.getDistrictId());
        }
        return ok;
    }

    /**
     * 根据 lighting_area 实际归属重新计算片区 spaceIds（逗号分隔的空间编码，去重）
     */
    private void refreshDistrictSpaceIds(Long districtId) {
        if (districtId == null) {
            return;
        }
        List<LightingArea> areas = super.list(new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getDistrictId, districtId));
        String spaceIds = areas.stream()
                .map(LightingArea::getSpace)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.joining(","));
        districtService.update(new LambdaUpdateWrapper<LightingDistrict>()
                .eq(LightingDistrict::getId, districtId)
                .set(LightingDistrict::getSpaceIds, spaceIds));
    }

    /**
     * 撤回区域MQ下发消息：只删除该区域下发、且未被消费的消息（共享队列不影响其他区域），并将待下发消息数清零
     * @param id 区域id
     * @return 撤回的消息总数
     */
    @Override
    public int recallMqMessages(Long id) {
        LightingArea area = super.getById(id);
        if (area == null) {
            throw new JeecgBootException("区域不存在");
        }
        Set<String> queues = resolveAreaQueues(area, null);
        Predicate<byte[]> matcher = buildAreaMessageMatcher(area, null);
        if (matcher == null) {
            log.warn("【MQ撤回】区域无可识别的下发消息（无回路或格式无法匹配），跳过：id={}, areaName={}", id, area.getAreaName());
        }
        int total = 0;
        for (String queueName : queues) {
            int recalled = matcher == null ? 0 : sendService.recallQueueMessages(queueName, matcher);
            log.info("【MQ撤回】区域：{}，队列：{}，撤回本区域未消费消息：{}条", area.getAreaName(), queueName, recalled);
            total += recalled;
        }
        // 撤回后待下发消息数清零
        super.update(new LambdaUpdateWrapper<LightingArea>()
                .eq(LightingArea::getId, id)
                .set(LightingArea::getPendingMsgCount, 0));
        log.info("【MQ撤回】完成：区域：{}，共撤回 {} 条", area.getAreaName(), total);
        return total;
    }

    /**
     * 构建按区域识别MQ下发消息的匹配器（匹配则命中/删除）：
     * - space=902（1号馆）：消息 GatewayAdr|KnxAdr 命中区域下回路编码提取的 IP|KNX地址
     * - space=903（北区）：消息 GatewayCode-CircuitCode 命中区域下回路编码
     * - space=904（新灯控）：消息 AreaID（=区域编码 area_code）命中本区域
     * - 其他空间：消息 AreaID（=区域编码）命中本区域
     * @param area 区域
     * @param circuits 区域下回路（可为null，内部按需查询；仅 902/903 需要）
     * @return 匹配器；区域无回路或消息无法识别时返回 null（不命中/不撤回任何消息）
     */
    private Predicate<byte[]> buildAreaMessageMatcher(LightingArea area, List<LightingCircuit> circuits) {
        if (area == null || StringUtils.isEmpty(area.getSpace())) {
            return null;
        }
        String space = area.getSpace();
        if ("902".equals(space)) {
            // 1号馆：按 GatewayAdr|KnxAdr 匹配区域下回路
            Set<String> circuitKeys = new HashSet<>();
            if (circuits == null) {
                circuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                        .eq(LightingCircuit::getAreaId, area.getId()));
            }
            for (LightingCircuit circuit : circuits) {
                String key = parseHgCircuitKey(circuit.getCircuitCode());
                if (key != null) {
                    circuitKeys.add(key);
                }
            }
            if (circuitKeys.isEmpty()) {
                return null;
            }
            return body -> {
                try {
                    JSONObject json = JSONObject.parseObject(new String(body));
                    String key = json.getString("GatewayAdr") + "|" + json.getString("KnxAdr");
                    return circuitKeys.contains(key);
                } catch (Exception e) {
                    return false;
                }
            };
        }
        if ("903".equals(space)) {
            // 北区：按 GatewayCode-CircuitCode 匹配区域下回路
            Set<String> circuitCodes = new HashSet<>();
            if (circuits == null) {
                circuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                        .eq(LightingCircuit::getAreaId, area.getId()));
            }
            for (LightingCircuit circuit : circuits) {
                if (StringUtils.isNotEmpty(circuit.getCircuitCode())) {
                    circuitCodes.add(circuit.getCircuitCode());
                }
            }
            if (circuitCodes.isEmpty()) {
                return null;
            }
            return body -> {
                try {
                    JSONObject json = JSONObject.parseObject(new String(body));
                    String fullCode = json.getString("GatewayCode") + "-" + json.getString("CircuitCode");
                    return circuitCodes.contains(fullCode);
                } catch (Exception e) {
                    return false;
                }
            };
        }
        if ("904".equals(space) || "905".equals(space)) {
            // 904/905（新灯控）：消息 AreaID = 区域编码（area_code），区域/回路消息都带，按区域编码精确匹配
            if (StringUtils.isEmpty(area.getAreaCode())) {
                return null;
            }
            final String areaCode = area.getAreaCode();
            return body -> {
                try {
                    String msgAreaId = JSONObject.parseObject(new String(body)).getString("AreaID");
                    return areaCode.equals(msgAreaId);
                } catch (Exception e) {
                    return false;
                }
            };
        }
        // 老空间：消息 AreaID = 区域编码（数值），匹配本区域
        if (!area.getAreaCode().matches("\\d+")) {
            return null;
        }
        final int areaCode = Integer.parseInt(area.getAreaCode());
        return body -> {
            try {
                Integer areaId = JSONObject.parseObject(new String(body)).getInteger("AreaID");
                return areaId != null && areaId.equals(areaCode);
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * 从1号馆回路编码中提取 GatewayAdr|KnxAdr 匹配键
     * 回路编码格式：Type=IpTunneling;HostAddress=10.22.133.32-4.1.25-3/7/18
     * 与 LightingSendService.send1hgControl 的提取规则一致
     */
    private String parseHgCircuitKey(String circuitCode) {
        if (StringUtils.isEmpty(circuitCode) || !circuitCode.contains("HostAddress=")) {
            return null;
        }
        int start = circuitCode.indexOf("HostAddress=") + "HostAddress=".length();
        int end = circuitCode.indexOf('-', start);
        if (end < 0) {
            end = circuitCode.length();
        }
        String ip = circuitCode.substring(start, end);
        int lastDash = circuitCode.lastIndexOf('-');
        if (lastDash < 0 || lastDash >= circuitCode.length() - 1) {
            return null;
        }
        String knxAdr = circuitCode.substring(lastDash + 1);
        return ip + "|" + knxAdr;
    }

    /**
     * 区域-全开
     * @param id 区域id
     */
    @Override
    @Transactional
    public void open(Long id) {
        control(id, true, null);
    }

    @Override
    @Transactional
    public void open(Long id, Long parentId) {
        control(id, true, parentId);
    }

    /**
     * 区域-全关
     * @param id 区域id
     */
    @Override
    @Transactional
    public void close(Long id) {
        control(id, false, null);
    }

    @Override
    @Transactional
    public void close(Long id, Long parentId) {
        control(id, false, parentId);
    }

    /**
     * mq-状态监听
     * @param areaCode 区域编码
     */
    @Override
    public void mqControl(String space,String areaCode,String value) {
        LightingArea area = super.getOne(new LambdaQueryWrapper<LightingArea>().eq(LightingArea::getSpace, space).eq(LightingArea::getAreaCode, areaCode));
        if(area == null){
            return;
        }
        // 判断状态
        String status = "";
        if(value.equals(area.getOpenCode())){
            status = "开启";
        }else if(value.equals(area.getCloseCode())){
            status = "关闭";
        }else {
            log.error("照明区域场景状态错误。space: {},areaCode: {},value: {}",space,areaCode,value);
            return;
        }

        super.update(new LambdaUpdateWrapper<LightingArea>().eq(LightingArea::getId,area.getId()).set(LightingArea::getStatus,status));
    }

    @Override
    public LightingArea getByCode(String space,String areaCode) {
        return getOne(new LambdaQueryWrapper<LightingArea>().eq(LightingArea::getSpace,space).eq(LightingArea::getAreaCode,areaCode));
    }

    @Override
    public List<LightingArea> getByIds(Collection<Long> ids) {
        if(CollectionUtil.isEmpty(ids)){
            return Collections.emptyList();
        }
        List<LightingArea> list = super.list(new LambdaQueryWrapper<LightingArea>().in(LightingArea::getId,ids));
        fillDistrictName(list);
        return list;
    }

    /**
     * 按区域查询1号馆的所有区域：查询条件写死为 space_name='1号馆'，id 参数仅为兼容前端调用（不参与查询）
     * 等价于 SELECT * FROM lighting_area WHERE space_name = '1号馆' ORDER BY sort
     */
    @Override
    public List<LightingArea> listBySpaceName(Long id) {
        List<LightingArea> list = super.list(new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getSpaceName, "1号馆")
                .orderByAsc(LightingArea::getSort)
                .orderByAsc(LightingArea::getId));
        fillDistrictName(list);
        return list;
    }

    /**
     * 按空间名称控制该空间下所有回路的开/关（走1号馆902控制逻辑）
     * 等价于 SELECT * FROM lighting_area WHERE space_name = #{spaceName}，
     * 再遍历各区域下所有回路，逐个调用 send1hgControl 下发
     */
    @Override
    public void controlBySpaceName(String spaceName, boolean type){
        if(StringUtils.isEmpty(spaceName)){
            throw new JeecgBootException("spaceName 不能为空");
        }

        List<LightingArea> areaList = super.list(new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getSpaceName, spaceName));
        if(CollectionUtil.isEmpty(areaList)){
            throw new JeecgBootException("未找到空间名称为【" + spaceName + "】的区域");
        }

        // 100=开，0=关（send1hgControl 内部把 100 转 true、0 转 false，传 "1" 会被误转成 false）
        String value = type ? "100" : "0";
        int sentCount = 0;
        int areaCount = 0;
        for(LightingArea area : areaList){
            // 查询区域下所有回路
            List<LightingCircuit> circuitList = circuitService.list(
                    new LambdaQueryWrapper<LightingCircuit>().eq(LightingCircuit::getAreaId, area.getId()));
            if(CollectionUtil.isEmpty(circuitList)){
                continue;
            }
            areaCount++;
            for(LightingCircuit circuit : circuitList){
                String circuitCode = circuit.getCircuitCode();
                if(StringUtils.isEmpty(circuitCode)){
                    continue;
                }
                try {
                    sendService.send1hgControl(circuitCode, value);
                    sentCount++;
                } catch (Exception e){
                    log.error("【1号馆】发送回路控制消息失败：circuitId={}, circuitName={}", circuit.getId(), circuit.getCircuitName(), e);
                }
            }
        }

        log.info("【1号馆】按空间控制完成：spaceName={}, 操作={}, 区域数={}, 下发回路数={}",
                spaceName, type ? "全开" : "全关", areaCount, sentCount);
    }

    @Override
    public void exportExcel(LightingAreaQueryDto params, HttpServletResponse response) {
        // 查询条件与 listPage1 一致，不分页查全量
        List<LightingArea> list = super.list(buildQueryWrapper(params));
        fillDistrictName(list);
        List<LightingAreaExportDto> rows = list.stream().map(this::toExportDto).collect(Collectors.toList());
        try (Workbook workbook = ExcelExportUtil.exportExcel(
                new ExportParams("区域列表", "区域列表", ExcelType.XSSF), LightingAreaExportDto.class, rows);
             OutputStream out = response.getOutputStream()) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("区域列表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")), "UTF-8")
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
            workbook.write(out);
            out.flush();
        } catch (IOException e) {
            log.error("导出区域列表Excel失败", e);
            throw new JeecgBootException("导出Excel失败");
        }
    }

    /**
     * 构建区域查询条件（listPage1 与导出共用）
     */
    private LambdaQueryWrapper<LightingArea> buildQueryWrapper(LightingAreaQueryDto params) {
        return new LambdaQueryWrapper<LightingArea>()
                .like(StringUtils.isNotEmpty(params.getRelName()), LightingArea::getRelName, params.getRelName())
                .eq(StringUtils.isNotEmpty(params.getSpace()), LightingArea::getSpace, params.getSpace())
                .eq(StringUtils.isNotEmpty(params.getSpaceName()), LightingArea::getSpaceName, params.getSpaceName())
                .like(StringUtils.isNotEmpty(params.getDeviceNo()), LightingArea::getDeviceNo, params.getDeviceNo())
                .eq(params.getDistrictId() != null, LightingArea::getDistrictId, params.getDistrictId())
                .like(StringUtils.isNotEmpty(params.getAreaName()), LightingArea::getAreaName, params.getAreaName())
                .orderByAsc(LightingArea::getSort)
                .orderByAsc(LightingArea::getId);
    }

    /**
     * 区域 → 导出行
     */
    private LightingAreaExportDto toExportDto(LightingArea area) {
        LightingAreaExportDto dto = new LightingAreaExportDto();
        BeanUtils.copyProperties(area, dto);
        dto.setStartTime(formatTime(area.getStartTime()));
        dto.setClosingTime(formatTime(area.getClosingTime()));
        return dto;
    }

    /**
     * 时间格式化
     */
    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 区域状态按回路实际状态计算：区域下任一回路为"开启"则区域为"开启"，否则为"关闭"。
     * （与 /bems/lighting/circuit/listPage?areaId=xx 返回的回路状态保持一致，只改出参不落库）
     */
    private void fillStatusByCircuit(List<LightingArea> areas) {
        if(CollectionUtil.isEmpty(areas)){
            return;
        }
        Set<Long> areaIds = areas.stream()
                .map(LightingArea::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if(areaIds.isEmpty()){
            return;
        }
        // 一次性查出这些区域下的所有回路
        List<LightingCircuit> circuits = circuitService.list(
                new LambdaQueryWrapper<LightingCircuit>().in(LightingCircuit::getAreaId, areaIds));
        // 有回路处于开启状态的区域ID集合
        Set<Long> openAreaIds = circuits.stream()
                .filter(c -> c.getAreaId() != null && LightingCircuit.STATUS_ON.equals(c.getStatus()))
                .map(LightingCircuit::getAreaId)
                .collect(Collectors.toSet());
        for(LightingArea area : areas){
            area.setStatus(openAreaIds.contains(area.getId())
                    ? LightingCircuit.STATUS_ON
                    : LightingCircuit.STATUS_OFF);
        }
    }

    /**
     * 区域通讯状态按区域下回路的通讯状态计算（任一回路在线则区域在线，无回路视为离线）
     */
    private void fillComstatByCircuit(List<LightingArea> areas) {
        if(CollectionUtil.isEmpty(areas)){
            return;
        }
        Set<Long> areaIds = areas.stream()
                .map(LightingArea::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if(areaIds.isEmpty()){
            return;
        }
        // 一次性查出这些区域下的所有回路
        List<LightingCircuit> circuits = circuitService.list(
                new LambdaQueryWrapper<LightingCircuit>().in(LightingCircuit::getAreaId, areaIds));
        // 有回路处于在线状态的区域ID集合
        Set<Long> onlineAreaIds = circuits.stream()
                .filter(c -> c.getAreaId() != null && LightingCircuit.COMSTAT_ONLINE.equals(c.getComstat()))
                .map(LightingCircuit::getAreaId)
                .collect(Collectors.toSet());
        for(LightingArea area : areas){
            area.setComstat(onlineAreaIds.contains(area.getId())
                    ? LightingCircuit.COMSTAT_ONLINE
                    : LightingCircuit.COMSTAT_OFFLINE);
        }
    }

    /**
     * 查询MQ中未被消费的下发消息数，更新待下发消息数字段（listPage1 时调用，同步落库）
     * 按区域精确统计：扫描区域下发涉及的队列，只统计该区域下发的消息（复用撤回接口的扫描逻辑），
     * 共享队列（如北区同网关、1号馆、老空间）不会把其他区域的消息算进来。
     * 区域下发涉及的队列：
     * - space=902（1号馆）：Lighting_operations
     * - space=903（北区）：lighting_control_bq_XX（按区域下各回路编码前缀的网关编号）
     * - 其他空间：按空间对应的单个队列
     */
    private void fillPendingMsgCount(List<LightingArea> areas) {
        if (CollectionUtil.isEmpty(areas)) {
            return;
        }
        Set<Long> areaIds = areas.stream()
                .map(LightingArea::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // 一次查出当前页区域下的所有回路（北区903需要按回路编码解析网关队列）
        List<LightingCircuit> circuits = CollectionUtil.isEmpty(areaIds) ? Collections.emptyList()
                : circuitService.list(new LambdaQueryWrapper<LightingCircuit>().in(LightingCircuit::getAreaId, areaIds));
        Map<Long, List<LightingCircuit>> circuitsByArea = circuits.stream()
                .filter(c -> c.getAreaId() != null)
                .collect(Collectors.groupingBy(LightingCircuit::getAreaId));

        for (LightingArea area : areas) {
            List<LightingCircuit> areaCircuits = circuitsByArea.getOrDefault(area.getId(), Collections.emptyList());
            Set<String> queues = resolveAreaQueues(area, areaCircuits);
            // 按区域精确统计：只统计该区域下发、且未被消费的消息
            Predicate<byte[]> matcher = buildAreaMessageMatcher(area, areaCircuits);
            int pending = 0;
            if (matcher != null) {
                for (String queueName : queues) {
                    pending += sendService.countQueueMessages(queueName, matcher);
                }
            }
            area.setPendingMsgCount(pending);
            // 同步落库，供后续查询/导出直接使用
            super.update(new LambdaUpdateWrapper<LightingArea>()
                    .eq(LightingArea::getId, area.getId())
                    .set(LightingArea::getPendingMsgCount, pending));
        }
    }

    /**
     * 解析区域下发所涉及的MQ队列
     * @param area 区域
     * @param circuits 区域下回路（可为null，内部按需查询；仅北区903需要）
     * @return 队列名集合
     */
    private Set<String> resolveAreaQueues(LightingArea area, List<LightingCircuit> circuits) {
        Set<String> queues = new HashSet<>();
        if (area == null || StringUtils.isEmpty(area.getSpace())) {
            return queues;
        }
        String space = area.getSpace();
        if ("902".equals(space)) {
            // 1号馆：全部走 Lighting_operations 队列
            queues.add(LightingMqConstant.QUEUE_LIGHTING_SEND_1HG);
        } else if ("903".equals(space)) {
            // 北区：按回路编码前缀的网关编号拼队列（lighting_control_bq_XX）
            if (circuits == null) {
                circuits = circuitService.list(new LambdaQueryWrapper<LightingCircuit>()
                        .eq(LightingCircuit::getAreaId, area.getId()));
            }
            for (LightingCircuit circuit : circuits) {
                String gatewayCode = parseBqGatewayCode(circuit.getCircuitCode());
                if (gatewayCode != null) {
                    queues.add(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_PREFIX + gatewayCode);
                }
            }
        } else if ("904".equals(space)) {
            // 904（新灯控）：GatewayCode固定54，全部走 lighting_control_bq_54 队列
            queues.add(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_54);
        } else if ("905".equals(space)) {
            // 905（新灯控）：GatewayCode固定154.100，全部走 lighting_control_bq_154_100 队列
            queues.add(LightingMqConstant.QUEUE_LIGHTING_SEND_BQ_905);
        } else {
            // 老空间：区域开/关为整区域场景消息，发到空间对应的单个队列
            String queueName = resolveSpaceQueue(space);
            if (queueName != null) {
                queues.add(queueName);
            }
        }
        return queues;
    }

    /**
     * 从北区回路编码（11-20）中解析网关编号，非法格式返回 null
     */
    private String parseBqGatewayCode(String circuitCode) {
        if (StringUtils.isEmpty(circuitCode)) {
            return null;
        }
        int dashIndex = circuitCode.indexOf('-');
        if (dashIndex <= 0) {
            return null;
        }
        String gatewayCode = circuitCode.substring(0, dashIndex);
        return gatewayCode.matches("\\d+") ? gatewayCode : null;
    }

    /**
     * 老空间区域控制队列（与 LightingSendService.send 的 gatewayCode 映射保持一致）
     */
    private String resolveSpaceQueue(String space) {
        switch (space) {
            case "1":
                return LightingMqConstant.QUEUE_LIGHTING_SEND;      // 金安桥
            case "2":
                return LightingMqConstant.QUEUE_LIGHTING_SEND_YGL;  // 一高炉
            case "3":
                return LightingMqConstant.QUEUE_LIGHTING_SEND_039;  // 039
            case "4":
                return LightingMqConstant.QUEUE_LIGHTING_SEND_DTT;  // 大跳台
            default:
                return null;
        }
    }

    /**
     * 回填片区名称
     */
    private void fillDistrictName(List<LightingArea> areas) {
        if(CollectionUtil.isEmpty(areas)){
            return;
        }
        Set<Long> districtIds = areas.stream()
                .map(LightingArea::getDistrictId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if(districtIds.isEmpty()){
            return;
        }
        Map<Long, String> nameMap = districtService.listByIds(districtIds).stream()
                .collect(Collectors.toMap(LightingDistrict::getId, LightingDistrict::getDistrictName, (a, b) -> a));
        for(LightingArea area : areas){
            if(area.getDistrictId() != null){
                area.setDistrictName(nameMap.get(area.getDistrictId()));
            }
        }
    }

    private void control(Long id, boolean type, Long parentId){
        LightingArea area = super.getById(id);
        if(area == null){
            throw new JeecgBootException("区域不存在");
        }

        // 1号馆（space=902）走新的MQ转发通道，不走老的KNX
        if("902".equals(area.getSpace())){
            control1hg(area, type);
        } else if("901".equals(area.getSpace())){
            // 四高炉（space=901）走电箱控制小程序通道，按 area_code（deviceSn）发送
            controlSgf(area, type);
        } else if("903".equals(area.getSpace())){
            // 北区（space=903）走新的MQ转发通道
            controlBq(area, type);
        } else if("904".equals(area.getSpace())){
            // 904（新灯控）走新的MQ转发通道，区域级消息（LightDataType=3）
            control904(area, type);
        } else if("905".equals(area.getSpace())){
            // 905（新灯控）走新的MQ转发通道，区域级消息（DataType=0，AreaID=区域编码）
            control905(area, type);
        } else {
            // 其他空间（金安桥=1、一高炉=2等）走老的KNX通道
            if(type) {
                service.areaOpen(area.getSpace(),area.getAreaCode(),area.getOpenCode());
            }else {
                service.areaClose(area.getSpace(),area.getAreaCode(),area.getCloseCode());
            }
        }

        // 记录区域操作日志（logType=区域）
        String operationType = type ? "区域全开" : "区域全关";
        LightingOperationLog areaLog = new LightingOperationLog();
        areaLog.setLogType(LightingOperationLog.LOG_TYPE_AREA);
        areaLog.setParentId(parentId);
        areaLog.setRelType(LightingOperationLog.REL_TYPE_AREA);
        areaLog.setRelId(id);
        areaLog.setName(area.getAreaName());
        areaLog.setOperationTime(LocalDateTime.now());
        areaLog.setOperationType(operationType);
        String operatorType = lightingOperationLogService.resolveOperatorType(parentId);
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
        areaLog.setOperationBy(operationBy);
        areaLog.setOperatorType(operatorType);
        lightingOperationLogService.save(areaLog);

        // 查询区域下所有回路，记录回路子日志
        List<LightingCircuit> circuitList = circuitService.list(
                new LambdaQueryWrapper<LightingCircuit>().eq(LightingCircuit::getAreaId, id));
        if (CollectionUtil.isNotEmpty(circuitList)) {
            List<LightingOperationLog> childLogs = new java.util.ArrayList<>();
            String circuitOpType = type ? "回路开启" : "回路关闭";
            for (LightingCircuit circuit : circuitList) {
                // 记录回路操作人/操作时间（区域全开/全关不改变回路状态，状态以设备回传为准）
                circuitService.recordControlOperator(circuit, operationBy);
                LightingOperationLog childLog = new LightingOperationLog();
                childLog.setLogType(LightingOperationLog.LOG_TYPE_CIRCUIT);
                childLog.setParentId(areaLog.getId());
                childLog.setRelType(LightingOperationLog.REL_TYPE_CIRCUIT);
                childLog.setRelId(circuit.getId());
                childLog.setName(area.getAreaName() + "-" + circuit.getCircuitName());
                childLog.setOperationTime(LocalDateTime.now());
                childLog.setOperationType(circuitOpType);
                childLog.setOperationBy(operationBy);
                childLog.setOperatorType(operatorType);
                childLogs.add(childLog);
            }
            lightingOperationLogService.saveBatchLog(childLogs);
        }
    }

    /**
     * 1号馆区域控制（走MQ转发小程序通道）
     * 遍历区域下所有回路，逐个发送控制消息
     */
    private void control1hg(LightingArea area, boolean type){
        // 查询区域下所有回路
        LambdaQueryWrapper<LightingCircuit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LightingCircuit::getAreaId, area.getId());
        java.util.List<LightingCircuit> circuitList = circuitService.list(wrapper);

        if(CollectionUtil.isEmpty(circuitList)){
            log.warn("【1号馆】区域下没有回路，areaId={}, areaName={}", area.getId(), area.getAreaName());
            return;
        }

        // 100=开，0=关（send1hgControl 内部把 100 转 true、0 转 false，传 "1" 会被误转成 false）
        String value = type ? "100" : "0";
        log.info("【1号馆】区域控制：areaName={}, 操作={}, 回路数={}", area.getAreaName(), type ? "全开" : "全关", circuitList.size());

        for(LightingCircuit circuit : circuitList){
            try {
                String circuitCode = circuit.getCircuitCode();
                if(StringUtils.isEmpty(circuitCode)){
                    continue;
                }

                sendService.send1hgControl(circuitCode, value);
            } catch (Exception e){
                log.error("【1号馆】发送回路控制消息失败：circuitId={}, circuitName={}", circuit.getId(), circuit.getCircuitName(), e);
            }
        }

        log.info("【1号馆】区域控制完成：areaName={}, 操作={}", area.getAreaName(), type ? "全开" : "全关");
    }

    /**
     * 北区（space=903）区域控制（走MQ转发小程序通道）
     * 遍历区域下所有回路，逐个发送控制消息
     */
    private void controlBq(LightingArea area, boolean type){
        // 查询区域下所有回路
        LambdaQueryWrapper<LightingCircuit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LightingCircuit::getAreaId, area.getId());
        java.util.List<LightingCircuit> circuitList = circuitService.list(wrapper);

        if(CollectionUtil.isEmpty(circuitList)){
            log.warn("【北区】区域下没有回路，areaId={}, areaName={}", area.getId(), area.getAreaName());
            return;
        }

        String value = type ? "100" : "0";
        log.info("【北区】区域控制：areaName={}, 操作={}, 回路数={}", area.getAreaName(), type ? "全开" : "全关", circuitList.size());

        for(LightingCircuit circuit : circuitList){
            try {
                String circuitCode = circuit.getCircuitCode();
                if(StringUtils.isEmpty(circuitCode)){
                    continue;
                }

                sendService.sendBqControl(circuitCode, value);
            } catch (Exception e){
                log.error("【北区】发送回路控制消息失败：circuitId={}, circuitName={}", circuit.getId(), circuit.getCircuitName(), e);
            }
        }

        log.info("【北区】区域控制完成：areaName={}, 操作={}", area.getAreaName(), type ? "全开" : "全关");
    }

    /**
     * 904空间区域/场景控制（走MQ转发小程序通道）
     * 区域级消息：DataType=3，仅带 AreaID，不带 CircuitCode；Value：1=开，12=关
     */
    private void control904(LightingArea area, boolean type){
        String value = type ? "1" : "12";
        log.info("【904】区域控制：areaName={}, 操作={}", area.getAreaName(), type ? "全开" : "全关");

        try {
            sendService.send904Control(area.getAreaCode(), null, value);
        } catch (Exception e){
            log.error("【904】发送区域控制消息失败：areaId={}, areaName={}", area.getId(), area.getAreaName(), e);
        }

        log.info("【904】区域控制完成：areaName={}, 操作={}", area.getAreaName(), type ? "全开" : "全关");
    }

    /**
     * 905（新灯控）区域控制：走新的MQ转发通道（lighting_control_bq_154_100）
     * 消息格式：{"DataType":"0","AreaID":区域编码,"Value":"0|1","GatewayCode":"154.100"}
     * AreaID=lighting_area.area_code；Value：0=开、1=关
     */
    private void control905(LightingArea area, boolean type){
        String areaCode = area.getAreaCode();
        if(StringUtils.isEmpty(areaCode)){
            log.warn("【905】区域编码为空，无法控制：areaId={}, areaName={}", area.getId(), area.getAreaName());
            return;
        }
        // 0=开、1=关
        String value = type ? "0" : "1";
        log.info("【905】区域控制：areaName={}, 操作={}, areaCode={}", area.getAreaName(), type ? "全开" : "全关", areaCode);

        try {
            sendService.send905Control(areaCode, value);
        } catch (Exception e){
            log.error("【905】发送区域控制消息失败：areaId={}, areaName={}, areaCode={}", area.getId(), area.getAreaName(), areaCode, e);
        }

        log.info("【905】区域控制完成：areaName={}, 操作={}", area.getAreaName(), type ? "全开" : "全关");
    }

    /**
     * 四高炉（space=901）区域控制（走电箱控制小程序通道）
     * 四高炉区域下无回路，区域本身对应一个电箱设备，area_code 即设备编号 deviceSn（如 yel_power_sg06）
     */
    private void controlSgf(LightingArea area, boolean type){
        String areaCode = area.getAreaCode();
        if(StringUtils.isEmpty(areaCode)){
            log.warn("【四高炉】区域编码为空，无法控制：areaId={}, areaName={}", area.getId(), area.getAreaName());
            return;
        }

        String onOff = type ? "1" : "0";
        log.info("【四高炉】区域控制：areaName={}, 操作={}, deviceSn={}", area.getAreaName(), type ? "全开" : "全关", areaCode);

        try {
            sendService.sendSgfControl(areaCode, onOff);
        } catch (Exception e){
            log.error("【四高炉】发送电箱控制消息失败：areaId={}, areaName={}, deviceSn={}", area.getId(), area.getAreaName(), areaCode, e);
        }

        log.info("【四高炉】区域控制完成：areaName={}, 操作={}", area.getAreaName(), type ? "全开" : "全关");
    }
}
