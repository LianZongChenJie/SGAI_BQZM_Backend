package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.lighting.dto.LightingSceneDetailDto;
import org.jeecg.modules.bems.lighting.dto.LightingSceneDto;
import org.jeecg.modules.bems.lighting.dto.LightingSceneQueryDto;
import org.jeecg.modules.bems.lighting.dto.LightingSpaceScenesVo;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.entity.LightingScene;
import org.jeecg.modules.bems.lighting.entity.LightingSceneDetail;
import org.jeecg.modules.bems.lighting.mapper.LightingSceneDetailMapper;
import org.jeecg.modules.bems.lighting.mapper.LightingSceneMapper;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.ILightingSceneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    @Override
    public IPage<LightingPlan> listPage(LightingSceneQueryDto params) {
        // 名称过滤兼容前端只换 URL：planName 与 sceneName 等价，取任一非空值
        String name = StringUtils.isNotEmpty(params.getPlanName()) ? params.getPlanName() : params.getSceneName();
        Page<LightingScene> scenePage = super.page(new Page<>(params.getPageNo(), params.getPageSize()),
                new LambdaQueryWrapper<LightingScene>()
                        .like(StringUtils.isNotEmpty(name), LightingScene::getSceneName, name)
                        .eq(StringUtils.isNotEmpty(params.getSceneType()), LightingScene::getSceneType, params.getSceneType())
                        .eq(StringUtils.isNotEmpty(params.getStatus()), LightingScene::getStatus, params.getStatus())
                        .eq(StringUtils.isNotEmpty(params.getGroupId()), LightingScene::getGroupId, params.getGroupId())
                        .eq(StringUtils.isNotEmpty(params.getTagId()), LightingScene::getTagId, params.getTagId())
                        .like(StringUtils.isNotEmpty(params.getTagName()), LightingScene::getTagName, params.getTagName())
                        .orderByAsc(LightingScene::getSort)
        );
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

        List<LightingPlan> records = scenes.stream()
                .map(scene -> toPlan(scene, detailMap.getOrDefault(scene.getId(), new ArrayList<>())))
                .collect(Collectors.toList());
        page.setRecords(records);
        return page;
    }

    /**
     * 场景 → 照明计划结构映射（与 plan/listPage 出参字段一致）
     */
    private LightingPlan toPlan(LightingScene scene, List<LightingSceneDetail> details) {
        LightingPlan plan = new LightingPlan();
        plan.setId(scene.getId());
        plan.setPlanName(scene.getSceneName());
        plan.setPlanType(scene.getSceneType());
        plan.setStatus(scene.getStatus());
        plan.setSort(scene.getSort());
        plan.setRemark(scene.getRemark());
        plan.setGroupId(scene.getGroupId());
        plan.setTagId(scene.getTagId());
        plan.setTagName(scene.getTagName());
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
        scene.setStatus(LightingScene.STATUS_ENABLE);
        scene.setSort(dto.getSort() == null ? getMaxSort() + 1 : dto.getSort());
        scene.setRemark(dto.getRemark());
        scene.setGroupId(dto.getGroupId());
        scene.setTagId(dto.getTagId());
        scene.setTagName(dto.getTagName());
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
        scene.setSort(dto.getSort());
        scene.setRemark(dto.getRemark());
        scene.setGroupId(dto.getGroupId());
        scene.setTagId(dto.getTagId());
        scene.setTagName(dto.getTagName());
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
        dto.setGroupId(scene.getGroupId());
        dto.setTagId(scene.getTagId());
        dto.setTagName(scene.getTagName());
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
        if (CollectionUtil.isEmpty(details)) {
            throw new JeecgBootException("场景下没有控制目标，请先添加明细");
        }

        // 记录场景操作日志（父日志）
        String operationType = details.get(0).getOperationType();
        LightingOperationLog sceneLog = new LightingOperationLog();
        sceneLog.setLogType(LightingOperationLog.LOG_TYPE_SCENE);
        sceneLog.setParentId(null);
        sceneLog.setRelType("场景");
        sceneLog.setRelId(id);
        sceneLog.setName(scene.getSceneName());
        sceneLog.setOperationTime(java.time.LocalDateTime.now());
        sceneLog.setOperationType("场景" + operationType);
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
        if (CollectionUtil.isEmpty(dto.getDetails())) {
            throw new JeecgBootException("场景明细不能为空，至少添加一个控制目标");
        }
        for (LightingSceneDetail detail : dto.getDetails()) {
            if (!LightingScene.REL_TYPE_AREA.equals(detail.getRelType()) && !LightingScene.REL_TYPE_CIRCUIT.equals(detail.getRelType())) {
                throw new JeecgBootException("场景明细 relType 必须为 区域 或 回路");
            }
            if (!LightingScene.OPERATION_TYPE_OPEN.equals(detail.getOperationType()) && !LightingScene.OPERATION_TYPE_CLOSE.equals(detail.getOperationType())) {
                throw new JeecgBootException("场景明细 operationType 必须为 开启 或 关闭");
            }
            if (detail.getRelId() == null) {
                throw new JeecgBootException("场景明细 relId 不能为空");
            }
        }
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
}
