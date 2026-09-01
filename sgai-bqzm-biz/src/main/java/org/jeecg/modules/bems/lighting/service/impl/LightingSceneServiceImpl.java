package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.bems.lighting.dto.LightingPlanExportDto;
import org.jeecg.modules.bems.lighting.dto.LightingSceneDetailDto;
import org.jeecg.modules.bems.lighting.dto.LightingSceneDto;
import org.jeecg.modules.bems.lighting.dto.LightingSceneQueryDto;
import org.jeecg.modules.bems.lighting.dto.LightingSpaceScenesVo;
import org.jeecgframework.poi.excel.ExcelExportUtil;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.enmus.ExcelType;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.entity.LightingProgram;
import org.jeecg.modules.bems.lighting.entity.LightingScene;
import org.jeecg.modules.bems.lighting.entity.LightingSceneDetail;
import org.jeecg.modules.bems.lighting.mapper.LightingSceneDetailMapper;
import org.jeecg.modules.bems.lighting.mapper.LightingSceneMapper;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.ILightingProgramService;
import org.jeecg.modules.bems.lighting.service.ILightingSceneService;
import org.jeecg.modules.bems.lighting.service.YelIotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 照明场景 Service 实现
 * 场景独立存储于 lighting_scene / lighting_scene_detail 表，不绑定定时任务，仅用于一键开关灯。
 * listPage 出参映射为 LightingPlan 结构，与 /bems/lighting/plan/listPage 字段一致，前端只需换 URL。
 */
@Service
@AllArgsConstructor
@Slf4j
public class LightingSceneServiceImpl extends ServiceImpl<LightingSceneMapper, LightingScene> implements ILightingSceneService {

    private final LightingSceneDetailMapper detailMapper;

    private final ILightingAreaService lightingAreaService;

    private final ILightingCircuitService lightingCircuitService;

    private final ILightingOperationLogService lightingOperationLogService;

    private final LightingSendService lightingSendService;

    private final YelIotService yelIotService;

    private final ILightingProgramService lightingProgramService;

    @Override
    public IPage<LightingPlan> listPage(LightingSceneQueryDto params) {
        // 名称过滤兼容前端只换 URL：planName 与 sceneName 等价，取任一非空值
        String name = StringUtils.isNotEmpty(params.getPlanName()) ? params.getPlanName() : params.getSceneName();
        LambdaQueryWrapper<LightingScene> wrapper = new LambdaQueryWrapper<LightingScene>()
                        .like(StringUtils.isNotEmpty(name), LightingScene::getSceneName, name)
                        .eq(StringUtils.isNotEmpty(params.getSceneType()), LightingScene::getSceneType, params.getSceneType())
                        .eq(StringUtils.isNotEmpty(params.getCategory()), LightingScene::getCategory, params.getCategory())
                        .eq(StringUtils.isNotEmpty(params.getStatus()), LightingScene::getStatus, params.getStatus())
                        .eq(StringUtils.isNotEmpty(params.getTagId()), LightingScene::getTagId, params.getTagId())
                        .like(StringUtils.isNotEmpty(params.getTagName()), LightingScene::getTagName, params.getTagName())
                        .like(StringUtils.isNotEmpty(params.getProgramSceneIds()), LightingScene::getProgramSceneIds, params.getProgramSceneIds())
                        .orderByAsc(LightingScene::getSort);
        Page<LightingScene> scenePage = super.page(new Page<>(params.getPageNo(), params.getPageSize()), wrapper);
        List<LightingScene> scenes = scenePage.getRecords();
        Page<LightingPlan> page = new Page<>(scenePage.getCurrent(), scenePage.getSize(), scenePage.getTotal());
        if (CollectionUtil.isEmpty(scenes)) {
            return page;
        }
        // 批量查询明细（避免 N+1）
        List<Long> sceneIds = scenes.stream().map(LightingScene::getId).collect(Collectors.toList());
        List<LightingSceneDetail> allDetails = detailMapper.selectList(
                new LambdaQueryWrapper<LightingSceneDetail>().in(LightingSceneDetail::getSceneId, sceneIds));
        Map<Long, List<LightingSceneDetail>> detailMap = allDetails.stream()
                .collect(Collectors.groupingBy(LightingSceneDetail::getSceneId));
        // 批量加载本页场景引用的节目（避免 N+1），用于回填泛光节目ID(groupId) 展示
        Map<Long, LightingProgram> programMap = loadReferencedPrograms(scenes);

        List<LightingPlan> records = scenes.stream()
                .map(scene -> toPlan(scene, detailMap.getOrDefault(scene.getId(), new ArrayList<>()), programMap))
                .collect(Collectors.toList());
        // 回填当前正在运行的节目名称（查询泛光总控系统 get_group_run_state）
        fillRunningProgramDetail(records);
        page.setRecords(records);
        return page;
    }

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
            log.warn("【场景列表】判断 bqzm 角色失败，按非 bqzm 处理", e);
        }
        return false;
    }

    /**
     * 计算"完全归属于片区8(服贸会)"的场景id集合（供非 bqzm 角色过滤）
     * 归属规则：场景所有明细都落在片区8内。
     *  - 区域类型明细：relId ∈ 片区8区域集合
     *  - 回路类型明细：relId ∈ 片区8回路集合（回路归属区域 → 片区）
     *  - 无明细或存在任意明细不在片区8内 → 不归属
     */
    private Set<Long> computeDistrictSceneIds() {
        Set<Long> result = new HashSet<>();
        // 1. 片区8下所有区域
        List<LightingArea> districtAreas = lightingAreaService.list(
                new LambdaQueryWrapper<LightingArea>().eq(LightingArea::getDistrictId, 8L));
        if (CollectionUtil.isEmpty(districtAreas)) {
            return result;
        }
        Set<Long> districtAreaIds = districtAreas.stream()
                .map(LightingArea::getId).collect(Collectors.toSet());
        // 2. 片区8下所有回路（通过区域关联）
        List<LightingCircuit> districtCircuits = lightingCircuitService.list(
                new LambdaQueryWrapper<LightingCircuit>().in(LightingCircuit::getAreaId, districtAreaIds));
        Set<Long> districtCircuitIds = districtCircuits.stream()
                .map(LightingCircuit::getId).collect(Collectors.toSet());
        // 3. 所有场景明细，按 sceneId 分组
        List<LightingSceneDetail> allDetails = detailMapper.selectList(null);
        if (CollectionUtil.isEmpty(allDetails)) {
            return result;
        }
        Map<Long, List<LightingSceneDetail>> detailMap = allDetails.stream()
                .collect(Collectors.groupingBy(LightingSceneDetail::getSceneId));
        // 4. 判断每个场景是否完全归属片区8
        for (Map.Entry<Long, List<LightingSceneDetail>> entry : detailMap.entrySet()) {
            List<LightingSceneDetail> details = entry.getValue();
            if (CollectionUtil.isEmpty(details)) {
                continue;
            }
            boolean allInDistrict = true;
            for (LightingSceneDetail detail : details) {
                if (LightingScene.REL_TYPE_AREA.equals(detail.getRelType())) {
                    if (!districtAreaIds.contains(detail.getRelId())) {
                        allInDistrict = false;
                        break;
                    }
                } else if (LightingScene.REL_TYPE_CIRCUIT.equals(detail.getRelType())) {
                    if (!districtCircuitIds.contains(detail.getRelId())) {
                        allInDistrict = false;
                        break;
                    }
                } else {
                    // 未知明细类型，视为不在片区8内
                    allInDistrict = false;
                    break;
                }
            }
            if (allInDistrict) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * 批量加载场景引用的节目（关联 lighting_program.id），返回 id -> LightingProgram 映射
     */
    private Map<Long, LightingProgram> loadReferencedPrograms(List<LightingScene> scenes) {
        Set<Long> programIds = new HashSet<>();
        for (LightingScene scene : scenes) {
            if (StringUtils.isNotEmpty(scene.getProgramSceneIds())) {
                for (String s : scene.getProgramSceneIds().split(",")) {
                    if (StringUtils.isBlank(s.trim())) {
                        continue;
                    }
                    try {
                        programIds.add(Long.parseLong(s.trim()));
                    } catch (NumberFormatException e) {
                        log.warn("programSceneIds 包含非法ID，已忽略: {}", s);
                    }
                }
            }
        }
        if (CollectionUtil.isEmpty(programIds)) {
            return Collections.emptyMap();
        }
        return lightingProgramService.listByIds(programIds).stream()
                .collect(Collectors.toMap(LightingProgram::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * 解析场景引用的节目的名称列表，用于出参 programNames 展示；无引用返回空列表
     */
    private List<String> resolveProgramNames(String programSceneIds, Map<Long, LightingProgram> programMap) {
        if (StringUtils.isEmpty(programSceneIds) || programMap == null || programMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (String s : programSceneIds.split(",")) {
            if (StringUtils.isBlank(s.trim())) {
                continue;
            }
            try {
                LightingProgram program = programMap.get(Long.parseLong(s.trim()));
                if (program != null && StringUtils.isNotBlank(program.getProgramName()) && !names.contains(program.getProgramName())) {
                    names.add(program.getProgramName());
                }
            } catch (NumberFormatException ignored) {
                // 非法ID忽略
            }
        }
        return names;
    }

    /**
     * 回填场景列表的节目运行详情：查询泛光总控系统当前正在运行的节目（get_group_run_state），
     * 匹配每个场景引用的节目（programSceneIds，关联 lighting_program.id）的 groupId，
     * 把运行中（state=1）的节目名称写入 programDetail。
     * 总控系统不可达/未配置 host 时返回空列表，不影响列表查询。
     */
    private void fillRunningProgramDetail(List<LightingPlan> records) {
        if (CollectionUtil.isEmpty(records)) {
            return;
        }
        // 查询当前正在运行的节目（state=1），不做 groupId 匹配，全部展示
        List<String> runningNames = new ArrayList<>();
        for (JSONObject running : yelIotService.getRunningGroups()) {
            String groupName = running.getString("groupName");
            if (StringUtils.isNotEmpty(groupName) && !runningNames.contains(groupName)) {
                runningNames.add(groupName);
            }
        }
        for (LightingPlan record : records) {
            record.setProgramDetail(runningNames);
        }
    }

    @Override
    public void exportExcel(LightingSceneQueryDto params, HttpServletResponse response) {
        // 名称过滤兼容前端只换 URL：planName 与 sceneName 等价，取任一非空值
        String name = StringUtils.isNotEmpty(params.getPlanName()) ? params.getPlanName() : params.getSceneName();
        LambdaQueryWrapper<LightingScene> wrapper = new LambdaQueryWrapper<LightingScene>()
                .like(StringUtils.isNotEmpty(name), LightingScene::getSceneName, name)
                .eq(StringUtils.isNotEmpty(params.getSceneType()), LightingScene::getSceneType, params.getSceneType())
                .eq(StringUtils.isNotEmpty(params.getCategory()), LightingScene::getCategory, params.getCategory())
                .eq(StringUtils.isNotEmpty(params.getStatus()), LightingScene::getStatus, params.getStatus())
                .eq(StringUtils.isNotEmpty(params.getTagId()), LightingScene::getTagId, params.getTagId())
                .like(StringUtils.isNotEmpty(params.getTagName()), LightingScene::getTagName, params.getTagName())
                .like(StringUtils.isNotEmpty(params.getProgramSceneIds()), LightingScene::getProgramSceneIds, params.getProgramSceneIds())
                .orderByAsc(LightingScene::getSort);
        // 数据权限：bqzm 角色导出全部；其他角色仅导出完全归属于片区8的场景
        if (!isBqzmRole()) {
            Set<Long> allowedSceneIds = computeDistrictSceneIds();
            if (CollectionUtil.isEmpty(allowedSceneIds)) {
                allowedSceneIds = new HashSet<>();
                allowedSceneIds.add(-1L);
            }
            wrapper.in(LightingScene::getId, allowedSceneIds);
        }
        List<LightingScene> scenes = super.list(wrapper);
        List<LightingPlanExportDto> rows = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(scenes)) {
            // 批量查询明细（避免 N+1）
            List<Long> sceneIds = scenes.stream().map(LightingScene::getId).collect(Collectors.toList());
            Map<Long, List<LightingSceneDetail>> detailMap = detailMapper.selectList(
                            new LambdaQueryWrapper<LightingSceneDetail>().in(LightingSceneDetail::getSceneId, sceneIds))
                    .stream().collect(Collectors.groupingBy(LightingSceneDetail::getSceneId));
            // 批量加载场景引用的节目（用于回填泛光节目ID展示）
            Map<Long, LightingProgram> programMap = loadReferencedPrograms(scenes);
            rows = scenes.stream()
                    .map(scene -> toPlan(scene, detailMap.getOrDefault(scene.getId(), new ArrayList<>()), programMap))
                    .map(this::toExportDto)
                    .collect(Collectors.toList());
        }
        writeExcel("场景列表", LightingPlanExportDto.class, rows, response);
    }

    private LightingPlanExportDto toExportDto(LightingPlan plan) {
        LightingPlanExportDto dto = new LightingPlanExportDto();
        dto.setPlanName(plan.getPlanName());
        dto.setRelType(plan.getRelType());
        dto.setRelIds(plan.getRelIds());
        dto.setExecutionTime(plan.getExecutionTime());
        dto.setOperationType(plan.getOperationType());
        dto.setStatus(plan.getStatus());
        dto.setPlanType(plan.getPlanType());
        dto.setCycleType(plan.getCycleType());
        dto.setCategory(plan.getCategory());
        dto.setTagId(plan.getTagId());
        dto.setTagName(plan.getTagName());
        // 场景本身没有 groupId（groupId 只属于节目），导出不填
        dto.setProgramSceneIds(plan.getProgramSceneIds());
        dto.setSort(plan.getSort());
        dto.setRemark(plan.getRemark());
        dto.setCreateBy(plan.getCreateBy());
        dto.setCreateTime(formatDateTime(plan.getCreateTime()));
        dto.setUpdateBy(plan.getUpdateBy());
        dto.setUpdateTime(formatDateTime(plan.getUpdateTime()));
        return dto;
    }

    private void writeExcel(String title, Class<?> clazz, List<?> rows, HttpServletResponse response) {
        try (Workbook workbook = ExcelExportUtil.exportExcel(
                new ExportParams(title, title, ExcelType.XSSF), clazz, rows);
             OutputStream out = response.getOutputStream()) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode(title + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")), "UTF-8")
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
            workbook.write(out);
            out.flush();
        } catch (IOException e) {
            log.error("导出" + title + "Excel失败", e);
            throw new JeecgBootException("导出Excel失败");
        }
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 场景 → 照明计划结构映射（与 plan/listPage 出参字段一致）
     * programMap：场景引用节目的 id -> LightingProgram 映射，用于回填泛光节目ID(groupId) 展示
     */
    private LightingPlan toPlan(LightingScene scene, List<LightingSceneDetail> details, Map<Long, LightingProgram> programMap) {
        LightingPlan plan = new LightingPlan();
        plan.setId(scene.getId());
        plan.setPlanName(scene.getSceneName());
        plan.setPlanType(scene.getSceneType());
        plan.setCategory(scene.getCategory());
        plan.setStatus(scene.getStatus());
        plan.setSort(scene.getSort());
        plan.setRemark(scene.getRemark());
        // 场景本身没有 groupId（groupId 只属于节目 lighting_program.group_id），场景出参统一置空
        plan.setGroupId(null);
        plan.setProgramNames(resolveProgramNames(scene.getProgramSceneIds(), programMap));
        plan.setTagId(scene.getTagId());
        plan.setTagName(scene.getTagName());
        plan.setProgramSceneIds(scene.getProgramSceneIds());
        plan.setCreateBy(scene.getCreateBy());
        plan.setCreateTime(scene.getCreateTime());
        plan.setUpdateBy(scene.getUpdateBy());
        plan.setUpdateTime(scene.getUpdateTime());
        plan.setSysOrgCode(scene.getSysOrgCode());
        plan.setPageNo(scene.getPageNo());
        plan.setPageSize(scene.getPageSize());
        // 聚合明细：relType/operationType 取第一条（前端入参为统一类型），relIds 逗号拼接
        if (CollectionUtil.isNotEmpty(details)) {
            LightingSceneDetail first = details.get(0);
            plan.setRelType(first.getRelType());
            plan.setOperationType(first.getOperationType());
            plan.setRelIds(details.stream()
                    .map(d -> String.valueOf(d.getRelId()))
                    .collect(Collectors.joining(",")));
        }
        // 场景无定时配置：executionTime/executionInfo 保持 null，前端结构与 plan/listPage 一致
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(LightingSceneDto dto) {
        convertParams(dto);
        validate(dto);
        LightingScene scene = new LightingScene();
        scene.setSceneName(resolveSceneName(dto));
        scene.setSceneType(StringUtils.isEmpty(dto.getSceneType()) ? "普通场景" : dto.getSceneType());
        scene.setCategory(dto.getCategory());
        scene.setStatus(LightingScene.STATUS_ENABLE);
        scene.setSort(dto.getSort() == null ? getMaxSort() + 1 : dto.getSort());
        scene.setRemark(dto.getRemark());
        scene.setTagId(dto.getTagId());
        scene.setTagName(dto.getTagName());
        scene.setProgramSceneIds(dto.getProgramSceneIds());
        super.save(scene);
        saveDetails(scene.getId(), dto.getDetails());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(LightingSceneDto dto) {
        if (dto.getId() == null) {
            throw new JeecgBootException("场景id不能为空");
        }
        LightingScene old = super.getById(dto.getId());
        if (old == null) {
            throw new JeecgBootException("场景不存在");
        }
        convertParams(dto);
        validate(dto);
        LightingScene scene = new LightingScene();
        scene.setId(dto.getId());
        scene.setSceneName(resolveSceneName(dto));
        scene.setSceneType(StringUtils.isEmpty(dto.getSceneType()) ? old.getSceneType() : dto.getSceneType());
        scene.setCategory(dto.getCategory());
        scene.setSort(dto.getSort());
        scene.setRemark(dto.getRemark());
        scene.setTagId(dto.getTagId());
        scene.setTagName(dto.getTagName());
        scene.setProgramSceneIds(dto.getProgramSceneIds());
        super.updateById(scene);
        // 先删后插明细
        detailMapper.delete(new LambdaQueryWrapper<LightingSceneDetail>().eq(LightingSceneDetail::getSceneId, dto.getId()));
        saveDetails(dto.getId(), dto.getDetails());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LightingScene scene = super.getById(id);
        if (scene == null) {
            throw new JeecgBootException("场景不存在");
        }
        // 类别为 一键开关、节目 的场景禁止删除
        if (LightingScene.CATEGORY_ONE_CLICK_SWITCH.equals(scene.getCategory())
                || LightingScene.CATEGORY_PROGRAM.equals(scene.getCategory())) {
            throw new JeecgBootException("【" + scene.getCategory() + "】类别的场景不允许删除");
        }
        detailMapper.delete(new LambdaQueryWrapper<LightingSceneDetail>().eq(LightingSceneDetail::getSceneId, id));
        super.removeById(id);
    }

    /**
     * 场景详情（出参结构同 plan/detail，便于前端只换 URL）：
     * id/planName/relType/operationType/status/relName/areaList/circuitList，
     * 场景无定时配置，executionTime/startDate/endDate/enabledWeek/version 保持 null。
     */
    @Override
    public LightingSceneDetailDto getDetail(Long id) {
        LightingScene scene = super.getById(id);
        if (scene == null) {
            throw new JeecgBootException("场景不存在");
        }
        List<LightingSceneDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<LightingSceneDetail>()
                        .eq(LightingSceneDetail::getSceneId, id)
                        .orderByAsc(LightingSceneDetail::getSort));
        // 场景设计上明细类型统一（relType+relIds 入参），relType/operationType 取第一条即可

        LightingSceneDetailDto dto = new LightingSceneDetailDto();
        dto.setId(scene.getId());
        dto.setPlanName(scene.getSceneName());
        dto.setStatus(scene.getStatus());
        dto.setCategory(scene.getCategory());
        dto.setTagId(scene.getTagId());
        dto.setTagName(scene.getTagName());
        dto.setProgramSceneIds(scene.getProgramSceneIds());
        // 回填引用的节目的泛光节目ID/节目名称（展示用，取自 lighting_program）
        if (StringUtils.isNotEmpty(scene.getProgramSceneIds())) {
            Map<Long, LightingProgram> programMap = loadReferencedPrograms(Collections.singletonList(scene));
            // 场景本身没有 groupId（groupId 只属于节目），只回填节目名称，groupId 保持 null
            dto.setProgramNames(resolveProgramNames(scene.getProgramSceneIds(), programMap));
        }
        if (CollectionUtil.isNotEmpty(details)) {
            LightingSceneDetail first = details.get(0);
            dto.setRelType(first.getRelType());
            dto.setOperationType(first.getOperationType());
            List<Long> relIds = details.stream()
                    .map(LightingSceneDetail::getRelId)
                    .collect(Collectors.toList());
            if (LightingScene.REL_TYPE_AREA.equals(first.getRelType())) {
                List<LightingArea> areas = lightingAreaService.getByIds(relIds);
                dto.setAreaList(areas);
                // 顶层 relName 取第一个目标的 relName（区域所属灯组，如"室外高杆路灯"）
                if (CollectionUtil.isNotEmpty(areas)) {
                    dto.setRelName(areas.get(0).getRelName());
                }
            } else if (LightingScene.REL_TYPE_CIRCUIT.equals(first.getRelType())) {
                List<LightingCircuit> circuits = lightingCircuitService.listByIds(relIds);
                // 回填区域名/空间名，与 plan/detail 保持一致
                Map<Long, LightingArea> areaMap = lightingAreaService.listByIds(
                                circuits.stream().map(LightingCircuit::getAreaId).collect(Collectors.toSet()))
                        .stream().collect(Collectors.toMap(LightingArea::getId, Function.identity()));
                for (LightingCircuit circuit : circuits) {
                    LightingArea area = areaMap.get(circuit.getAreaId());
                    if (area != null) {
                        circuit.setAreaName(area.getAreaName());
                        circuit.setSpaceName(area.getSpaceName());
                    }
                }
                dto.setCircuitList(circuits);
                // 顶层 relName 取第一个回路的名称
                if (CollectionUtil.isNotEmpty(circuits)) {
                    dto.setRelName(circuits.get(0).getCircuitName());
                }
            }
        }
        return dto;
    }

    /**
     * 一键执行场景：按明细逐个控制目标（区域/回路开或关），自动记录控制日志。
     * 注意：不加 @Transactional——与老引擎 LightingPlanServiceImpl.execution() 一致，
     * 避免"MQ 已下发但日志被回滚"的不一致（MQ 消息不是事务资源，不会随事务回滚）。
     */
    @Override
    public void apply(Long id) {
        LightingScene scene = super.getById(id);
        if (scene == null) {
            throw new JeecgBootException("场景不存在");
        }
        List<LightingSceneDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<LightingSceneDetail>()
                        .eq(LightingSceneDetail::getSceneId, id)
                        .orderByAsc(LightingSceneDetail::getSort));
        // 明细为空但引用了节目时也允许执行
        if (CollectionUtil.isEmpty(details) && StringUtils.isEmpty(scene.getProgramSceneIds())) {
            throw new JeecgBootException("场景下没有控制目标，请先添加明细");
        }

        // 节目统一执行的操作类型：取第一条明细的操作类型（与父日志一致），无明细默认开启
        String operationType = CollectionUtil.isNotEmpty(details) ? details.get(0).getOperationType() : LightingScene.OPERATION_TYPE_OPEN;
        // 记录场景操作日志（父日志）
        LightingOperationLog sceneLog = buildSceneLog(scene, operationType, id);
        lightingOperationLogService.save(sceneLog);

        log.info("开始执行场景【{}】, 目标数：{}", scene.getSceneName(), details.size());
        for (LightingSceneDetail detail : details) {
            if (LightingScene.REL_TYPE_AREA.equals(detail.getRelType())) {
                executeArea(detail, sceneLog.getId());
            } else if (LightingScene.REL_TYPE_CIRCUIT.equals(detail.getRelType())) {
                executeCircuit(detail, sceneLog.getId());
            } else {
                throw new JeecgBootException("场景明细 relType 必须为 区域 或 回路");
            }
        }
        // 执行引用的节目（节目按 groupId 发泛光节目MQ，日志挂在父日志下）
        executeProgramScenes(scene.getProgramSceneIds(), operationType, sceneLog.getId());
    }

    /**
     * 场景全开/全关：根据场景id和操作类型（开启/关闭 或 OPEN/CLOSE），
     * 对场景下所有区域、回路统一执行开或关（忽略明细各自的 operationType），自动记录控制日志。
     * 不加 @Transactional，与 apply/老引擎 execution() 一致，避免 MQ 已下发但日志被回滚的不一致。
     */
    @Override
    public void control(Long sceneId, String operationType) {
        if (sceneId == null) {
            throw new JeecgBootException("场景id不能为空");
        }
        // 操作类型兼容：开启/关闭 或 OPEN/CLOSE
        String op = operationType;
        if ("OPEN".equalsIgnoreCase(op)) {
            op = LightingScene.OPERATION_TYPE_OPEN;
        } else if ("CLOSE".equalsIgnoreCase(op)) {
            op = LightingScene.OPERATION_TYPE_CLOSE;
        }
        if (!LightingScene.OPERATION_TYPE_OPEN.equals(op) && !LightingScene.OPERATION_TYPE_CLOSE.equals(op)) {
            throw new JeecgBootException("operationType 必须为 开启/关闭 或 OPEN/CLOSE");
        }
        LightingScene scene = super.getById(sceneId);
        if (scene == null) {
            throw new JeecgBootException("场景不存在");
        }
        List<LightingSceneDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<LightingSceneDetail>()
                        .eq(LightingSceneDetail::getSceneId, sceneId)
                        .orderByAsc(LightingSceneDetail::getSort));
        // 明细为空但引用了节目时也允许控制
        if (CollectionUtil.isEmpty(details) && StringUtils.isEmpty(scene.getProgramSceneIds())) {
            throw new JeecgBootException("场景下没有控制目标，请先添加明细");
        }

        // 记录场景操作日志（父日志，操作类型=场景），子日志通过 parentId 自动继承
        LightingOperationLog sceneLog = buildSceneLog(scene, op, sceneId);
        lightingOperationLogService.save(sceneLog);

        log.info("场景【{}】{} 控制开始，目标数：{}", scene.getSceneName(), op, details.size());
        for (LightingSceneDetail detail : details) {
            if (LightingScene.REL_TYPE_AREA.equals(detail.getRelType())) {
                executeArea(detail, sceneLog.getId(), op);
            } else if (LightingScene.REL_TYPE_CIRCUIT.equals(detail.getRelType())) {
                executeCircuit(detail, sceneLog.getId(), op);
            } else {
                throw new JeecgBootException("场景明细 relType 必须为 区域 或 回路");
            }
        }
        // 执行引用的节目（节目按 groupId 发泛光节目MQ，日志挂在父日志下）
        executeProgramScenes(scene.getProgramSceneIds(), op, sceneLog.getId());
    }

    /**
     * 执行场景引用的节目：programSceneIds 为 lighting_program.id 集合（节目表），
     * 节目不查区域/回路明细，而是按 groupId（泛光节目ID）发送MQ给小程序控制（onOff：1开2关）。
     */
    private void executeProgramScenes(String programSceneIds, String op, Long parentLogId) {
        if (StringUtils.isEmpty(programSceneIds)) {
            return;
        }
        List<Long> programIds = new ArrayList<>();
        for (String s : programSceneIds.split(",")) {
            if (StringUtils.isBlank(s.trim())) {
                continue;
            }
            try {
                programIds.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException e) {
                log.warn("programSceneIds 包含非法ID，已忽略: {}", s);
            }
        }
        if (programIds.isEmpty()) {
            return;
        }
        List<LightingProgram> programs = lightingProgramService.listByIds(programIds);
        if (programs.size() < programIds.size()) {
            log.warn("场景引用节目 {} 个，实际找到 {} 个，缺失的ID已跳过", programIds.size(), programs.size());
        }
        for (LightingProgram program : programs) {
            // 节目按 groupId（泛光节目ID）发 MQ 给小程序，不走区域/回路明细
            if (StringUtils.isNotBlank(program.getGroupId())) {
                int onOff = LightingScene.OPERATION_TYPE_OPEN.equals(op) ? 1 : 2;
                log.info("执行节目【{}】{}，发送泛光节目MQ：groupId={}", program.getProgramName(), op, program.getGroupId());
                lightingSendService.sendGroupOper(program.getGroupId(), onOff, program.getId(), program.getProgramName());
                // 记录节目控制日志（挂在父日志下）
                lightingOperationLogService.save(buildProgramLog(program, op, parentLogId));
            } else {
                log.warn("节目【{}】未配置泛光节目ID(groupId)，跳过", program.getProgramName());
            }
        }
    }

    /**
     * 构建节目控制子日志（logType=节目、operatorType=场景、name=节目名、operationType=节目+开/关），
     * 挂在场景控制父日志（parentId）下，与区域/回路子日志同一层级。
     */
    private LightingOperationLog buildProgramLog(LightingProgram program, String op, Long parentLogId) {
        LightingOperationLog programLog = new LightingOperationLog();
        programLog.setLogType(LightingOperationLog.LOG_TYPE_PROGRAM);
        programLog.setParentId(parentLogId);
        programLog.setRelType("场景");
        programLog.setRelId(program.getId());
        programLog.setName(program.getProgramName());
        programLog.setOperationTime(java.time.LocalDateTime.now());
        programLog.setOperationType("节目" + op);
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
        programLog.setOperationBy(operationBy);
        programLog.setOperatorType(LightingOperationLog.OPERATOR_TYPE_SCENE);
        return programLog;
    }

    /**
     * 构建场景操作父日志（logType=场景、operatorType=场景、name=场景名、operationType=场景+开/关），
     * apply 与 control 共用，子日志通过 parentId 自动继承操作类型。
     */
    private LightingOperationLog buildSceneLog(LightingScene scene, String op, Long relId) {
        LightingOperationLog sceneLog = new LightingOperationLog();
        sceneLog.setLogType(LightingOperationLog.LOG_TYPE_SCENE);
        sceneLog.setParentId(null);
        sceneLog.setRelType("场景");
        sceneLog.setRelId(relId);
        sceneLog.setName(scene.getSceneName());
        sceneLog.setOperationTime(java.time.LocalDateTime.now());
        sceneLog.setOperationType("场景" + op);
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
        sceneLog.setOperationBy(operationBy);
        sceneLog.setOperatorType(LightingOperationLog.OPERATOR_TYPE_SCENE);
        return sceneLog;
    }

    /**
     * 区域目标开/关（内部复用区域控制逻辑，自动记录控制日志，操作人为当前登录用户）
     */
    private void executeArea(LightingSceneDetail detail, Long parentId) {
        if (LightingScene.OPERATION_TYPE_OPEN.equals(detail.getOperationType())) {
            lightingAreaService.open(detail.getRelId(), parentId);
        } else if (LightingScene.OPERATION_TYPE_CLOSE.equals(detail.getOperationType())) {
            lightingAreaService.close(detail.getRelId(), parentId);
        } else {
            throw new JeecgBootException("场景明细 operationType 必须为 开启 或 关闭");
        }
    }

    /**
     * 回路目标开/关（内部复用回路控制逻辑，自动记录控制日志，操作人为当前登录用户）
     */
    private void executeCircuit(LightingSceneDetail detail, Long parentId) {
        if (LightingScene.OPERATION_TYPE_OPEN.equals(detail.getOperationType())) {
            lightingCircuitService.open(detail.getRelId(), parentId);
        } else if (LightingScene.OPERATION_TYPE_CLOSE.equals(detail.getOperationType())) {
            lightingCircuitService.close(detail.getRelId(), parentId);
        } else {
            throw new JeecgBootException("场景明细 operationType 必须为 开启 或 关闭");
        }
    }

    /**
     * 区域目标按指定操作类型开/关（场景全开/全关：忽略明细自身 operationType）
     */
    private void executeArea(LightingSceneDetail detail, Long parentId, String op) {
        if (LightingScene.OPERATION_TYPE_OPEN.equals(op)) {
            lightingAreaService.open(detail.getRelId(), parentId);
        } else {
            lightingAreaService.close(detail.getRelId(), parentId);
        }
    }

    /**
     * 回路目标按指定操作类型开/关（场景全开/全关：忽略明细自身 operationType）
     */
    private void executeCircuit(LightingSceneDetail detail, Long parentId, String op) {
        if (LightingScene.OPERATION_TYPE_OPEN.equals(op)) {
            lightingCircuitService.open(detail.getRelId(), parentId);
        } else {
            lightingCircuitService.close(detail.getRelId(), parentId);
        }
    }

    /**
     * 兼容前端现用入参：planName + relType + relIds + operationType 自动转换为明细列表。
     * 已传 details 明细列表时不做转换。
     * operationType 可不传：为空时默认"开启"（场景用于一键开灯，缺省按开灯处理）。
     */
    private void convertParams(LightingSceneDto dto) {
        if (CollectionUtil.isNotEmpty(dto.getDetails())) {
            // 明细列表格式：每条明细 operationType 为空时默认"开启"
            for (LightingSceneDetail detail : dto.getDetails()) {
                detail.setOperationType(resolveOperationType(detail.getOperationType()));
            }
            return;
        }
        if (StringUtils.isEmpty(dto.getRelIds())) {
            return;
        }
        // 操作类型兼容：开启/关闭 或 OPEN/CLOSE（与 LightingPlanServiceImpl.control 一致），为空默认"开启"
        dto.setOperationType(resolveOperationType(dto.getOperationType()));

        List<LightingSceneDetail> details = new ArrayList<>();
        String[] ids = dto.getRelIds().split(",");
        int sort = 0;
        for (String s : ids) {
            if (StringUtils.isBlank(s.trim())) {
                continue;
            }
            Long relId;
            try {
                relId = Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                throw new JeecgBootException("relIds 包含非法ID，无法解析: " + s);
            }
            LightingSceneDetail detail = new LightingSceneDetail();
            detail.setRelType(dto.getRelType());
            detail.setRelId(relId);
            detail.setOperationType(dto.getOperationType());
            detail.setSort((long) sort++);
            details.add(detail);
        }
        if (CollectionUtil.isEmpty(details)) {
            throw new JeecgBootException("relIds 不能为空");
        }
        dto.setDetails(details);
    }

    /**
     * 解析操作类型：开启/关闭 或 OPEN/CLOSE，空值默认"开启"
     */
    private String resolveOperationType(String operationType) {
        if (StringUtils.isEmpty(operationType)) {
            return LightingScene.OPERATION_TYPE_OPEN;
        }
        if ("OPEN".equalsIgnoreCase(operationType)) {
            return LightingScene.OPERATION_TYPE_OPEN;
        }
        if ("CLOSE".equalsIgnoreCase(operationType)) {
            return LightingScene.OPERATION_TYPE_CLOSE;
        }
        return operationType;
    }

    /**
     * 解析场景名称：优先 planName（前端现用），其次 sceneName
     */
    private String resolveSceneName(LightingSceneDto dto) {
        return StringUtils.isNotEmpty(dto.getPlanName()) ? dto.getPlanName() : dto.getSceneName();
    }

    /**
     * 保存明细，冗余名称便于展示
     */
    private void saveDetails(Long sceneId, List<LightingSceneDetail> details) {
        if (CollectionUtil.isEmpty(details)) {
            return;
        }
        enrichNames(details);
        int sort = 0;
        for (LightingSceneDetail detail : details) {
            detail.setId(null);
            detail.setSceneId(sceneId);
            if (detail.getSort() == null) {
                detail.setSort((long) sort);
            }
            detailMapper.insert(detail);
            sort++;
        }
    }

    /**
     * 按 relType + relId 回填名称（区域名 / 区域名-回路名）
     */
    private void enrichNames(List<LightingSceneDetail> details) {
        // 区域
        List<Long> areaIds = details.stream()
                .filter(d -> LightingScene.REL_TYPE_AREA.equals(d.getRelType()))
                .map(LightingSceneDetail::getRelId).collect(Collectors.toList());
        Map<Long, LightingArea> areaMap = CollectionUtil.isEmpty(areaIds) ? Map.of()
                : lightingAreaService.getByIds(areaIds).stream().collect(Collectors.toMap(LightingArea::getId, Function.identity()));

        // 回路
        List<Long> circuitIds = details.stream()
                .filter(d -> LightingScene.REL_TYPE_CIRCUIT.equals(d.getRelType()))
                .map(LightingSceneDetail::getRelId).collect(Collectors.toList());
        Map<Long, LightingCircuit> circuitMap = CollectionUtil.isEmpty(circuitIds) ? Map.of()
                : lightingCircuitService.listByIds(circuitIds).stream().collect(Collectors.toMap(LightingCircuit::getId, Function.identity()));
        Map<Long, LightingArea> circuitAreaMap = CollectionUtil.isEmpty(circuitMap) ? Map.of()
                : lightingAreaService.getByIds(circuitMap.values().stream().map(LightingCircuit::getAreaId).collect(Collectors.toSet()))
                        .stream().collect(Collectors.toMap(LightingArea::getId, Function.identity()));

        for (LightingSceneDetail detail : details) {
            if (LightingScene.REL_TYPE_AREA.equals(detail.getRelType())) {
                LightingArea area = areaMap.get(detail.getRelId());
                detail.setRelName(area == null ? detail.getRelName() : area.getAreaName());
            } else if (LightingScene.REL_TYPE_CIRCUIT.equals(detail.getRelType())) {
                LightingCircuit circuit = circuitMap.get(detail.getRelId());
                if (circuit != null) {
                    LightingArea area = circuitAreaMap.get(circuit.getAreaId());
                    detail.setRelName(area == null ? circuit.getCircuitName() : area.getAreaName() + "-" + circuit.getCircuitName());
                }
            }
        }
    }

    /**
     * 参数校验（场景名、明细非空、relType/operationType/relId 合法）+ 名称查重
     */
    private void validate(LightingSceneDto dto) {
        if (StringUtils.isEmpty(resolveSceneName(dto))) {
            throw new JeecgBootException("场景名称不能为空");
        }
        // 名称查重（编辑时排除自身），与 LightingPlan.add 的 check 保持一致
        Long selfId = dto.getId();
        if (count(new LambdaQueryWrapper<LightingScene>()
                .ne(selfId != null, LightingScene::getId, selfId)
                .eq(LightingScene::getSceneName, resolveSceneName(dto))) > 0) {
            throw new JeecgBootException("场景名称重复");
        }
//        if (CollectionUtil.isEmpty(dto.getDetails())) {
//            throw new JeecgBootException("场景明细不能为空，至少添加一个控制目标");
//        }
//        for (LightingSceneDetail detail : dto.getDetails()) {
//            if (!LightingScene.REL_TYPE_AREA.equals(detail.getRelType()) && !LightingScene.REL_TYPE_CIRCUIT.equals(detail.getRelType())) {
//                throw new JeecgBootException("场景明细 relType 必须为 区域 或 回路");
//            }
//            if (!LightingScene.OPERATION_TYPE_OPEN.equals(detail.getOperationType()) && !LightingScene.OPERATION_TYPE_CLOSE.equals(detail.getOperationType())) {
//                throw new JeecgBootException("场景明细 operationType 必须为 开启 或 关闭");
//            }
//            if (detail.getRelId() == null) {
//                throw new JeecgBootException("场景明细 relId 不能为空");
//            }
//        }
    }

    private Long getMaxSort() {
        Page<LightingScene> page = super.page(new Page<>(1, 1, false),
                new LambdaQueryWrapper<LightingScene>().orderByDesc(LightingScene::getSort));
        return CollectionUtil.isNotEmpty(page.getRecords()) ? page.getRecords().get(0).getSort() : 0;
    }

    /**
     * 按空间查询：该空间下的所有场景（含明细）和所有回路（含区域名/空间名）。
     * 场景归属规则：场景明细中任一目标（区域或回路）属于该空间即视为该空间的场景。
     * 注意：areaIds/circuitIds 为空时必须短路返回，避免 MyBatis-Plus 的 in(condition=false, ...) 跳过条件查出全表数据。
     */
    @Override
    public LightingSpaceScenesVo getBySpace(String spaceId) {
        if (StringUtils.isBlank(spaceId)) {
            throw new JeecgBootException("spaceId 不能为空");
        }
        LightingSpaceScenesVo vo = new LightingSpaceScenesVo();
        vo.setSpaceId(spaceId);

        // 1. 空间下所有区域
        List<LightingArea> areas = lightingAreaService.list(
                new LambdaQueryWrapper<LightingArea>().eq(LightingArea::getSpace, spaceId));
        String spaceName = null;
        for (LightingArea area : areas) {
            if (StringUtils.isNotEmpty(area.getSpaceName())) {
                spaceName = area.getSpaceName();
                break;
            }
        }
        vo.setSpaceName(spaceName);
        List<Long> areaIds = areas.stream().map(LightingArea::getId).collect(Collectors.toList());
        Map<Long, LightingArea> areaMap = areas.stream()
                .collect(Collectors.toMap(LightingArea::getId, Function.identity(), (a, b) -> a));

        // 2. 空间下所有回路（areaId 属于该空间），回填区域名/空间名
        List<LightingCircuit> circuits = Collections.emptyList();
        if (CollectionUtil.isNotEmpty(areaIds)) {
            circuits = lightingCircuitService.list(
                    new LambdaQueryWrapper<LightingCircuit>()
                            .in(LightingCircuit::getAreaId, areaIds)
                            .orderByAsc(LightingCircuit::getAreaId)
                            .orderByAsc(LightingCircuit::getId));
            for (LightingCircuit circuit : circuits) {
                LightingArea area = areaMap.get(circuit.getAreaId());
                if (area != null) {
                    circuit.setAreaName(area.getAreaName());
                    circuit.setSpaceName(area.getSpaceName());
                }
            }
        }
        vo.setCircuits(circuits);
        List<Long> circuitIds = circuits.stream().map(LightingCircuit::getId).collect(Collectors.toList());

        // 3. 空间下所有场景：明细目标是该空间的区域或回路
        if (CollectionUtil.isEmpty(areaIds) && CollectionUtil.isEmpty(circuitIds)) {
            vo.setScenes(Collections.emptyList());
            return vo;
        }
        Set<Long> sceneIds = new HashSet<>();
        if (CollectionUtil.isNotEmpty(areaIds)) {
            List<LightingSceneDetail> areaDetails = detailMapper.selectList(
                    new LambdaQueryWrapper<LightingSceneDetail>()
                            .eq(LightingSceneDetail::getRelType, LightingScene.REL_TYPE_AREA)
                            .in(LightingSceneDetail::getRelId, areaIds));
            areaDetails.forEach(d -> sceneIds.add(d.getSceneId()));
        }
        if (CollectionUtil.isNotEmpty(circuitIds)) {
            List<LightingSceneDetail> circuitDetails = detailMapper.selectList(
                    new LambdaQueryWrapper<LightingSceneDetail>()
                            .eq(LightingSceneDetail::getRelType, LightingScene.REL_TYPE_CIRCUIT)
                            .in(LightingSceneDetail::getRelId, circuitIds));
            circuitDetails.forEach(d -> sceneIds.add(d.getSceneId()));
        }
        if (sceneIds.isEmpty()) {
            vo.setScenes(Collections.emptyList());
            return vo;
        }

        List<LightingScene> scenes = super.listByIds(sceneIds);
        // 批量加载这些场景的全部明细（场景整体返回，前端按场景一键执行）
        Map<Long, List<LightingSceneDetail>> detailMap = detailMapper.selectList(
                        new LambdaQueryWrapper<LightingSceneDetail>()
                                .in(LightingSceneDetail::getSceneId, sceneIds)
                                .orderByAsc(LightingSceneDetail::getSort))
                .stream()
                .collect(Collectors.groupingBy(LightingSceneDetail::getSceneId));
        for (LightingScene scene : scenes) {
            scene.setDetails(detailMap.getOrDefault(scene.getId(), Collections.emptyList()));
        }
        scenes.sort(Comparator.comparing(LightingScene::getSort, Comparator.nullsLast(Long::compareTo)));
        vo.setScenes(scenes);
        return vo;
    }

    /**
     * 按区域查询场景（出参为 LightingPlan 结构，与 listPage 一致，含关联节目名称/运行中节目）。
     * 归属规则：场景明细包含该区域（relType=区域 且 relId=areaId），或包含该区域下的回路。
     * 注意：明细 in 查询需在集合非空时才执行，避免 MyBatis-Plus 跳过条件查出全表。
     */
    @Override
    public List<LightingPlan> listByArea(Long areaId) {
        if (areaId == null) {
            throw new JeecgBootException("areaId 不能为空");
        }
        // 区域不存在时自然查不到回路和明细，返回空列表，不报错
        // 1. 该区域下的回路
        List<Long> circuitIds = lightingCircuitService.list(
                        new LambdaQueryWrapper<LightingCircuit>().eq(LightingCircuit::getAreaId, areaId))
                .stream().map(LightingCircuit::getId).collect(Collectors.toList());

        // 2. 反查明细包含该区域（或该区域回路）的场景
        Set<Long> sceneIds = new HashSet<>();
        List<LightingSceneDetail> areaDetails = detailMapper.selectList(
                new LambdaQueryWrapper<LightingSceneDetail>()
                        .eq(LightingSceneDetail::getRelType, LightingScene.REL_TYPE_AREA)
                        .eq(LightingSceneDetail::getRelId, areaId));
        areaDetails.forEach(d -> sceneIds.add(d.getSceneId()));
        if (CollectionUtil.isNotEmpty(circuitIds)) {
            List<LightingSceneDetail> circuitDetails = detailMapper.selectList(
                    new LambdaQueryWrapper<LightingSceneDetail>()
                            .eq(LightingSceneDetail::getRelType, LightingScene.REL_TYPE_CIRCUIT)
                            .in(LightingSceneDetail::getRelId, circuitIds));
            circuitDetails.forEach(d -> sceneIds.add(d.getSceneId()));
        }
        if (sceneIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 查询场景 + 明细 + 关联节目，映射为 plan/listPage 出参结构（含节目名称/运行中节目）
        List<LightingScene> scenes = super.listByIds(sceneIds);
        scenes.sort(Comparator.comparing(LightingScene::getSort, Comparator.nullsLast(Long::compareTo)));
        Map<Long, List<LightingSceneDetail>> detailMap = detailMapper.selectList(
                        new LambdaQueryWrapper<LightingSceneDetail>()
                                .in(LightingSceneDetail::getSceneId, sceneIds)
                                .orderByAsc(LightingSceneDetail::getSort))
                .stream()
                .collect(Collectors.groupingBy(LightingSceneDetail::getSceneId));
        Map<Long, LightingProgram> programMap = loadReferencedPrograms(scenes);
        List<LightingPlan> records = scenes.stream()
                .map(scene -> toPlan(scene, detailMap.getOrDefault(scene.getId(), Collections.emptyList()), programMap))
                .collect(Collectors.toList());
        fillRunningProgramDetail(records);
        return records;
    }
}
