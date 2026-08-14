package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.lighting.dto.LightingPlanDetailDto;
import org.jeecg.modules.bems.lighting.dto.LightingPlanExportDto;
import org.jeecg.modules.bems.lighting.dto.LightingPlanQueryDto;
import org.jeecgframework.poi.excel.ExcelExportUtil;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.enmus.ExcelType;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecuteLog;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.entity.LightingProgram;
import org.jeecg.modules.bems.lighting.entity.LightingScene;
import org.jeecg.modules.bems.lighting.entity.LightingSceneDetail;
import org.jeecg.modules.bems.lighting.mapper.LightingPlanMapper;
import org.jeecg.modules.bems.lighting.mapper.LightingSceneDetailMapper;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.jeecg.modules.bems.lighting.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class LightingPlanServiceImpl extends ServiceImpl<LightingPlanMapper, LightingPlan> implements ILightingPlanService {
    private final LightingService lightingService;

    private final ILightingAreaService lightingAreaService;

    private final ILightingCircuitService lightingCircuitService;

    private final LightingSendService lightingSendService;

    private final ILightingPlanExecutionTimeService executionTimeService;

    private final ILightingSceneService lightingSceneService;

    private final ILightingProgramService lightingProgramService;

    private final ILightingOperationLogService lightingOperationLogService;

    private final ILightingPlanExecuteLogService lightingPlanExecuteLogService;

    private final LightingSceneDetailMapper lightingSceneDetailMapper;


    @Override
    public IPage<LightingPlan> listPage(LightingPlanQueryDto param) {
        Page<LightingPlan> page = super.page(new Page<>(param.getPageNo(), param.getPageSize()),
                new LambdaQueryWrapper<LightingPlan>()
                        .eq(StringUtils.isNotEmpty(param.getRelType()), LightingPlan::getRelType, param.getRelType())
                        .gt(StringUtils.isNotEmpty(param.getStartTime()), LightingPlan::getExecutionTime, param.getStartTime())
                        .lt(StringUtils.isNotEmpty(param.getEndTime()), LightingPlan::getExecutionTime, param.getEndTime())
                        .orderByAsc(LightingPlan::getSort)
        );
        List<LightingPlan> records = page.getRecords();
        if(CollectionUtil.isEmpty(records)){
            return page;
        }
        // 获取计划执行配置信息
        Map<Long, LightingPlanExecutionTime> executionTimeMap = executionTimeService.getByPlanIds(records.stream().map(LightingPlan::getId).toList())
                .stream().collect(Collectors.toMap(LightingPlanExecutionTime::getPlanId, Function.identity()));
        records.forEach(plan -> {
            plan.setExecutionInfo(executionTimeMap.get(plan.getId()));
        });
        return page;
    }

    @Override
    public void exportExcel(LightingPlanQueryDto param, HttpServletResponse response) {
        // 查询条件与 listPage 一致，不分页查全量
        List<LightingPlan> list = super.list(new LambdaQueryWrapper<LightingPlan>()
                .eq(StringUtils.isNotEmpty(param.getRelType()), LightingPlan::getRelType, param.getRelType())
                .gt(StringUtils.isNotEmpty(param.getStartTime()), LightingPlan::getExecutionTime, param.getStartTime())
                .lt(StringUtils.isNotEmpty(param.getEndTime()), LightingPlan::getExecutionTime, param.getEndTime())
                .orderByAsc(LightingPlan::getSort));
        Map<Long, LightingPlanExecutionTime> executionTimeMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(list)) {
            Map<Long, LightingPlanExecutionTime> fetched = executionTimeService.getByPlanIds(list.stream().map(LightingPlan::getId).toList())
                    .stream().collect(Collectors.toMap(LightingPlanExecutionTime::getPlanId, Function.identity()));
            executionTimeMap.putAll(fetched);
        }
        List<LightingPlanExportDto> rows = list.stream()
                .map(plan -> toExportDto(plan, executionTimeMap.get(plan.getId())))
                .collect(Collectors.toList());
        writeExcel("计划列表", LightingPlanExportDto.class, rows, response);
    }

    private LightingPlanExportDto toExportDto(LightingPlan plan, LightingPlanExecutionTime info) {
        LightingPlanExportDto dto = new LightingPlanExportDto();
        dto.setPlanName(plan.getPlanName());
        dto.setRelType(plan.getRelType());
        dto.setRelIds(plan.getRelIds());
        dto.setExecutionTime(plan.getExecutionTime());
        dto.setOperationType(plan.getOperationType());
        dto.setStatus(plan.getStatus());
        dto.setPlanType(plan.getPlanType());
        dto.setCycleType(plan.getCycleType());
        if (info != null) {
            dto.setStartDate(info.getStartDate());
            dto.setEndDate(info.getEndDate());
            dto.setEnabledWeek(info.getEnabledWeek());
        }
        dto.setTagId(plan.getTagId());
        dto.setTagName(plan.getTagName());
        dto.setGroupId(plan.getGroupId());
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

    @Override
    public void add(LightingPlan plan) {
        // 检验名称是否重复
        check(plan);
        // 增加各种校验
        plan.setStatus(LightingPlan.STATUS_DISABLE);
        // 设置排序字段
        if(plan.getSort() == null){
            plan.setSort(getMaxSort() + 1);
        }
        super.save(plan);
    }

    private Long getMaxSort(){
        Page<LightingPlan> page = super.page(new Page<>(1, 1, false), new LambdaQueryWrapper<LightingPlan>().orderByDesc(LightingPlan::getSort));
        return CollectionUtil.isNotEmpty(page.getRecords()) ? page.getRecords().get(0).getSort() : 0;
    }

    @Override
    public void edit(LightingPlan plan) {
        check(plan);
        LightingPlan old = super.getById(plan.getId());
        if(old == null){
            throw new JeecgBootException("计划不存在");
        }
        if (!LightingPlan.STATUS_DISABLE.equals(old.getStatus())) {
            throw new JeecgBootException("计划已启用，不能修改");
        }
        super.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LightingPlan plan = super.getById(id);
        // 定时任务同步的计划：同步删除对应的定时任务，避免数据不一致
        if(plan != null && plan.getScheduleJobId() != null){
            Long jobId = plan.getScheduleJobId();
            // 任务可能已被定时任务接口先删除，判空避免 stop 抛异常阻断计划删除
//            if(scheduleJobService.getById(jobId) != null){
//                scheduleJobService.stop(jobId);
//                scheduleJobService.removeById(jobId);
//            }
        }
        // 同步删除执行时间配置，避免孤儿数据
        executionTimeService.remove(new LambdaQueryWrapper<LightingPlanExecutionTime>().eq(LightingPlanExecutionTime::getPlanId, id));
        // 同步删除执行日志，避免孤儿数据
        lightingPlanExecuteLogService.remove(new LambdaQueryWrapper<LightingPlanExecuteLog>().eq(LightingPlanExecuteLog::getPlanId, id));
        super.removeById(id);
    }

    /**
     * 照明计划执行（MQ 消息消费端调用）
     * @param id 计划id
     * @param version 版本号
     * @return 是否执行成功（false 表示计划停用/版本不匹配/时间偏差超限等未实际执行）
     */
    @Override
    public boolean execution(Long id,String version) {
        // 获取计划信息
        LightingPlan plan = super.getById(id);
        if(plan == null || !LightingPlan.STATUS_ENABLE.equals(plan.getStatus())){
            log.warn("计划不存在或未启用，执行失败。计划id：{}", id);
            return false;
        }
        // 定时任务同步的计划：由动态调度器执行，跳过老引擎MQ执行
        if(plan.getScheduleJobId() != null){
            log.warn("计划由动态调度器执行，跳过老引擎MQ执行。计划id：{}", id);
            return false;
        }

        LightingPlanExecutionTime executionTime = executionTimeService.getByPlanIdAndVersion(id,version);
        if(executionTime == null){
            log.warn("执行时间配置不存在或版本不匹配，执行失败。计划id：{}，version：{}", id, version);
            return false;
        }

        LocalTime time = LocalTime.now();
        long between = Math.abs(ChronoUnit.SECONDS.between(time, executionTime.getExecutionLocalTime()));
        if(between > 300){
            log.error("当前时间与计划执行时间相差>300秒，执行失败。计划id：" + id);
            return false;
        }

        // 记录定时任务操作日志（父日志）；场景类型用"场景-定时任务"，其余用"定时任务"
        LightingOperationLog planLog = new LightingOperationLog();
        boolean isScenePlan = LightingPlan.REL_TYPE_SCENE.equals(plan.getRelType());
        planLog.setLogType(isScenePlan ? LightingOperationLog.LOG_TYPE_SCENE_PLAN : LightingOperationLog.LOG_TYPE_PLAN);
        planLog.setParentId(null);
        planLog.setRelType(isScenePlan ? LightingPlan.REL_TYPE_SCENE : "定时任务");
        planLog.setRelId(id);
        planLog.setName(plan.getPlanName());
        planLog.setOperationTime(java.time.LocalDateTime.now());
        planLog.setOperationType((isScenePlan ? "场景-定时任务" : "定时任务") + plan.getOperationType());
        planLog.setOperationBy("照明计划");
        planLog.setOperatorType(LightingOperationLog.OPERATOR_TYPE_PLAN);
        lightingOperationLogService.save(planLog);

        Set<Long> relIds = Arrays.stream(plan.getRelIds().split(",")).map(Long::parseLong).collect(Collectors.toSet());
        if(LightingPlan.REL_TYPE_AREA.equals(plan.getRelType())){
            // 区域（场景）：含引用节目的场景，节目按 groupId 发泛光节目MQ
            executeAreaWithPrograms(relIds, plan.getOperationType(), planLog.getId());
        }else if(LightingPlan.REL_TYPE_CIRCUIT.equals(plan.getRelType())){
            // 回路
            executeCircuit(relIds, plan.getOperationType(), planLog.getId());
        }else if(LightingPlan.REL_TYPE_SCENE.equals(plan.getRelType())){
            // 场景：按各自明细的开关执行，场景引用的节目按 groupId 发泛光节目MQ
            executeScenePlan(relIds, plan.getOperationType(), planLog.getId());
        }
        return true;
    }

    @Override
    @Transactional
    public void enable(LightingPlanExecutionTime data) {
        LightingPlan plan = super.getById(data.getPlanId());
        if(plan == null){
            throw new JeecgBootException("计划不存在");
        }
        // 定时任务同步的计划：委托给动态调度器启用，避免老引擎MQ与新调度器双重执行
        if(plan.getScheduleJobId() != null){
//            scheduleJobService.start(plan.getScheduleJobId());
            return;
        }
        plan.setStatus(LightingPlan.STATUS_ENABLE);
        plan.setExecutionTime(data.getExecutionTime());
        super.updateById(plan);
        executionTimeService.saveOrUpdate(data);
        plan.setExecutionTime(data.getExecutionTime());
        // 判断下次执行时间
        if (data.getExecutionLocalTime().isBefore(LocalTime.now())) {
            return;
        }
        LocalDate now = LocalDate.now();
        if(!data.getEnabledWeek().contains(String.valueOf(now.getDayOfWeek().getValue()))){
            return;
        }
        if(data.getStartLocalDate().isAfter(now) || data.getEndLocalDate().isBefore(now)){
            return;
        }
        lightingSendService.sendPlan(plan.getId(),plan.getPlanName(),data.getVersion(),now.atTime(data.getExecutionLocalTime()));
    }

    @Override
    public void disable(Long id) {
        LightingPlan plan = super.getById(id);
        if(plan == null){
            throw new JeecgBootException("计划不存在");
        }
        // 定时任务同步的计划：委托给动态调度器停用，避免数据不一致
        if(plan.getScheduleJobId() != null){
//            scheduleJobService.stop(plan.getScheduleJobId());
            return;
        }
        if(LightingPlan.STATUS_DISABLE.equals(plan.getStatus())){
            return;
        }
        plan.setStatus(LightingPlan.STATUS_DISABLE);
        super.updateById(plan);
    }

    private void executeArea(Collection<Long> areaIds, String operationType, Long parentId){
        for(Long areaId : areaIds){
            if(LightingPlan.OPERATION_TYPE_OPEN.equals(operationType)){
                lightingAreaService.open(areaId, parentId);
            }else if(LightingPlan.OPERATION_TYPE_CLOSE.equals(operationType)){
                lightingAreaService.close(areaId, parentId);
            }
        }
    }

    private void executeCircuit(Collection<Long> circuitIds, String operationType, Long parentId){
        for(Long circuitId : circuitIds){
            if(LightingPlan.OPERATION_TYPE_OPEN.equals(operationType)){
                lightingCircuitService.open(circuitId, parentId);
            }else if(LightingPlan.OPERATION_TYPE_CLOSE.equals(operationType)){
                lightingCircuitService.close(circuitId, parentId);
            }
        }
    }

    /**
     * 区域（场景）批量执行：与 control 保持一致——
     * 1. relIds 中命中 lighting_scene 且引用了节目（programSceneIds）的场景，从区域控制中移除，节目改由 executeProgramScenes 按 groupId 发泛光节目MQ；
     * 2. 其余目标（真实区域ID）走区域控制 executeArea。
     * 供定时执行 execution()/手动立即执行 executionNow() 复用。
     */
    private void executeAreaWithPrograms(Collection<Long> areaIds, String operationType, Long parentId){
        Set<Long> normalIds = new HashSet<>(areaIds); // 默认全部走区域控制
        List<org.jeecg.modules.bems.lighting.entity.LightingScene> sceneList = lightingSceneService.listByIds(areaIds);
        if(CollectionUtil.isNotEmpty(sceneList)){
            for(org.jeecg.modules.bems.lighting.entity.LightingScene scene : sceneList){
                if(StringUtils.isNotEmpty(scene.getProgramSceneIds())){
                    // 引用了节目的场景：节目走 executeProgramScenes，不走区域控制
                    normalIds.remove(scene.getId());
                }
            }
        }
        // relIds 直接传了节目ID（纯节目计划没有区域/回路明细，relIds 即引用的节目ID）→ 按节目执行
        if(!normalIds.isEmpty()){
            List<LightingProgram> directPrograms = lightingProgramService.listByIds(normalIds);
            if(CollectionUtil.isNotEmpty(directPrograms)){
                for(LightingProgram p : directPrograms){
                    normalIds.remove(p.getId());
                }
                executeProgramList(directPrograms, operationType, parentId);
            }
        }
        // 先执行真实区域目标
        if(!normalIds.isEmpty()){
            executeArea(normalIds, operationType, parentId);
        }
        // 再执行场景引用的节目（按 groupId 发泛光节目MQ，日志挂在父日志下）
        if(CollectionUtil.isNotEmpty(sceneList)){
            for(LightingScene scene : sceneList){
                executeProgramScenes(scene.getProgramSceneIds(), operationType, parentId, 0);
            }
        }
    }

    /**
     * 场景批量执行：relIds 为场景ID集合（lighting_scene.id）。
     * 1. 按各自明细的开关执行：遍历场景，对场景明细（区域/回路）按明细自身的 operationType 逐个控制；
     * 2. 场景引用的节目（programSceneIds）按 groupId 发泛光节目MQ，日志挂在父日志下；
     * 供定时执行 execution()/手动立即执行 executionNow() 复用。
     */
    private void executeScenePlan(Collection<Long> sceneIds, String operationType, Long parentId){
        if(CollectionUtil.isEmpty(sceneIds)){
            return;
        }
        List<LightingScene> sceneList = lightingSceneService.listByIds(sceneIds);
        if(sceneList.size() < sceneIds.size()){
            log.warn("场景计划引用场景 {} 个，实际找到 {} 个，缺失的ID已跳过", sceneIds.size(), sceneList.size());
        }
        for(LightingScene scene : sceneList){
            List<LightingSceneDetail> details = lightingSceneDetailMapper.selectList(
                    new LambdaQueryWrapper<LightingSceneDetail>()
                            .eq(LightingSceneDetail::getSceneId, scene.getId())
                            .orderByAsc(LightingSceneDetail::getSort));
            // 明细为空但引用了节目时也允许执行
            if(CollectionUtil.isEmpty(details) && StringUtils.isEmpty(scene.getProgramSceneIds())){
                log.warn("场景【{}】下没有控制目标，跳过", scene.getSceneName());
                continue;
            }
            // 按各自明细的 operationType 执行（区域/回路各自开或关）
            for(LightingSceneDetail detail : details){
                if(LightingScene.REL_TYPE_AREA.equals(detail.getRelType())){
                    executeArea(Collections.singleton(detail.getRelId()), detail.getOperationType(), parentId);
                }else if(LightingScene.REL_TYPE_CIRCUIT.equals(detail.getRelType())){
                    executeCircuit(Collections.singleton(detail.getRelId()), detail.getOperationType(), parentId);
                }else{
                    log.warn("场景【{}】明细 relType 非法：{}，已跳过", scene.getSceneName(), detail.getRelType());
                }
            }
            // 场景引用的节目（按 groupId 发泛光节目MQ，日志挂在父日志下）
            executeProgramScenes(scene.getProgramSceneIds(), operationType, parentId, 0);
        }
    }
    private void check(LightingPlan plan){
        if(count(new LambdaQueryWrapper<LightingPlan>().ne(plan.getId() != null, LightingPlan::getId, plan.getId()).eq(LightingPlan::getPlanName, plan.getPlanName())) > 0){
            throw new JeecgBootException("名称重复");
        }
    }

    @Override
    public LightingPlanDetailDto getDetail(Long id) {
        // 查询计划基本信息
        LightingPlan plan = super.getById(id);
        if (plan == null) {
            throw new JeecgBootException("计划不存在");
        }

        // 查询执行时间配置
        LightingPlanExecutionTime executionTime = executionTimeService.getByPlanId(id);

        // 构建 DTO
        LightingPlanDetailDto dto = new LightingPlanDetailDto();
        dto.setId(plan.getId());
        dto.setPlanName(plan.getPlanName());
        dto.setRelType(plan.getRelType());
        dto.setOperationType(plan.getOperationType());
        dto.setStatus(plan.getStatus());
        dto.setExecutionTime(plan.getExecutionTime());

        if (executionTime != null) {
            dto.setStartDate(executionTime.getStartDate());
            dto.setEndDate(executionTime.getEndDate());
            dto.setEnabledWeek(executionTime.getEnabledWeek());
            dto.setVersion(executionTime.getVersion());
        }

        // 查询关联信息
        if (StringUtils.isNotEmpty(plan.getRelIds())) {
            List<Long> relIds = Arrays.stream(plan.getRelIds().split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            if (LightingPlan.REL_TYPE_AREA.equals(plan.getRelType())) {
                // 查询区域列表
                dto.setAreaList(lightingAreaService.getByIds(relIds));
            } else if (LightingPlan.REL_TYPE_CIRCUIT.equals(plan.getRelType())) {
                // 查询回路列表
                List<LightingCircuit> circuits = lightingCircuitService.listByIds(relIds);
                // 查询区域
                Map<Long,LightingArea> areaMap = lightingAreaService.listByIds(circuits.stream().map(LightingCircuit::getAreaId).collect(Collectors.toSet()))
                                .stream()
                                        .collect(Collectors.toMap(LightingArea::getId,Function.identity()));
                for(LightingCircuit circuit : circuits){
                    LightingArea area = areaMap.get(circuit.getAreaId());
                    if(area != null){
                        circuit.setAreaName(area.getAreaName());
                        circuit.setSpaceName(area.getSpaceName());
                    }
                }
                dto.setCircuitList(circuits);
            } else if (LightingPlan.REL_TYPE_SCENE.equals(plan.getRelType())) {
                // 查询场景列表，并为每个场景回填明细（区域/回路目标，含名称），供前端直接展示
                List<LightingScene> scenes = lightingSceneService.listByIds(relIds);
                if (CollectionUtil.isNotEmpty(scenes)) {
                    List<Long> sceneIds = scenes.stream().map(LightingScene::getId).collect(Collectors.toList());
                    List<LightingSceneDetail> allDetails = lightingSceneDetailMapper.selectList(
                            new LambdaQueryWrapper<LightingSceneDetail>()
                                    .in(LightingSceneDetail::getSceneId, sceneIds)
                                    .orderByAsc(LightingSceneDetail::getSort));
                    fillSceneDetailRelNames(allDetails);
                    Map<Long, List<LightingSceneDetail>> detailMap = allDetails.stream()
                            .collect(Collectors.groupingBy(LightingSceneDetail::getSceneId));
                    for (LightingScene scene : scenes) {
                        scene.setDetails(detailMap.getOrDefault(scene.getId(), Collections.emptyList()));
                    }
                }
                dto.setSceneList(scenes);
            }
        }

        return dto;
    }

    /**
     * 回填场景明细的名称：relName 为空时按 relType 从区域/回路表补全（展示用）
     *
     * @param details 场景明细列表
     */
    private void fillSceneDetailRelNames(List<LightingSceneDetail> details) {
        if (CollectionUtil.isEmpty(details)) {
            return;
        }
        List<Long> areaIds = new ArrayList<>();
        List<Long> circuitIds = new ArrayList<>();
        for (LightingSceneDetail detail : details) {
            if (LightingScene.REL_TYPE_AREA.equals(detail.getRelType())) {
                areaIds.add(detail.getRelId());
            } else if (LightingScene.REL_TYPE_CIRCUIT.equals(detail.getRelType())) {
                circuitIds.add(detail.getRelId());
            }
        }
        Map<Long, LightingArea> areaMap = CollectionUtil.isNotEmpty(areaIds)
                ? lightingAreaService.listByIds(areaIds).stream().collect(Collectors.toMap(LightingArea::getId, Function.identity()))
                : Collections.emptyMap();
        Map<Long, LightingCircuit> circuitMap = CollectionUtil.isNotEmpty(circuitIds)
                ? lightingCircuitService.listByIds(circuitIds).stream().collect(Collectors.toMap(LightingCircuit::getId, Function.identity()))
                : Collections.emptyMap();
        for (LightingSceneDetail detail : details) {
            if (StringUtils.isNotEmpty(detail.getRelName())) {
                continue;
            }
            if (LightingScene.REL_TYPE_AREA.equals(detail.getRelType())) {
                LightingArea area = areaMap.get(detail.getRelId());
                if (area != null) {
                    detail.setRelName(area.getAreaName());
                }
            } else if (LightingScene.REL_TYPE_CIRCUIT.equals(detail.getRelType())) {
                LightingCircuit circuit = circuitMap.get(detail.getRelId());
                if (circuit != null) {
                    detail.setRelName(circuit.getCircuitName());
                }
            }
        }
    }

    /**
     * 现在执行
     *
     * @param id 计划id
     */
    @Override
    public void executionNow(Long id) {
        LightingPlan plan = super.getById(id);
        if(plan == null){
            throw new JeecgBootException("计划不存在");
        }
        // 定时任务同步的计划：委托给动态调度器立即执行，避免双重执行
        if(plan.getScheduleJobId() != null){
//            scheduleJobService.executeOnce(plan.getScheduleJobId());
            return;
        }

        // 记录定时任务操作日志（父日志）；场景类型用"场景-定时任务"，其余用"定时任务"
        LightingOperationLog planLog = new LightingOperationLog();
        boolean isScenePlan = LightingPlan.REL_TYPE_SCENE.equals(plan.getRelType());
        planLog.setLogType(isScenePlan ? LightingOperationLog.LOG_TYPE_SCENE_PLAN : LightingOperationLog.LOG_TYPE_PLAN);
        planLog.setParentId(null);
        planLog.setRelType(isScenePlan ? LightingPlan.REL_TYPE_SCENE : "定时任务");
        planLog.setRelId(id);
        planLog.setName(plan.getPlanName());
        planLog.setOperationTime(java.time.LocalDateTime.now());
        planLog.setOperationType((isScenePlan ? "场景-定时任务" : "定时任务") + plan.getOperationType());
        // 手动立即执行，取当前登录用户
        String operationBy = "照明计划";
        try {
            org.jeecg.common.system.vo.LoginUser sysUser = (org.jeecg.common.system.vo.LoginUser) org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
            if (sysUser != null) {
                operationBy = sysUser.getUsername();
            }
        } catch (Exception e) {
            // 异步场景中SecurityManager不可用，使用默认用户
        }
        planLog.setOperationBy(operationBy);
        planLog.setOperatorType(LightingOperationLog.OPERATOR_TYPE_MANUAL);
        lightingOperationLogService.save(planLog);

        Set<Long> relIds = Arrays.stream(plan.getRelIds().split(",")).map(Long::parseLong).collect(Collectors.toSet());
        if(LightingPlan.REL_TYPE_AREA.equals(plan.getRelType())){
            // 区域（场景）：含引用节目的场景，节目按 groupId 发泛光节目MQ
            executeAreaWithPrograms(relIds, plan.getOperationType(), planLog.getId());
        }else if(LightingPlan.REL_TYPE_CIRCUIT.equals(plan.getRelType())){
            // 回路
            executeCircuit(relIds, plan.getOperationType(), planLog.getId());
        }else if(LightingPlan.REL_TYPE_SCENE.equals(plan.getRelType())){
            // 场景：按各自明细的开关执行，场景引用的节目按 groupId 发泛光节目MQ
            executeScenePlan(relIds, plan.getOperationType(), planLog.getId());
        }
    }





    @Override
    public void removeByScheduleJobId(Long scheduleJobId) {
        if(scheduleJobId == null){
            return;
        }
        LightingPlan plan = super.getOne(new LambdaQueryWrapper<LightingPlan>().eq(LightingPlan::getScheduleJobId, scheduleJobId));
        if(plan == null){
            return;
        }
        executionTimeService.remove(new LambdaQueryWrapper<LightingPlanExecutionTime>().eq(LightingPlanExecutionTime::getPlanId, plan.getId()));
        super.removeById(plan.getId());
    }



    @Override
    public void control(String relType, String relIds, String operationType, Long sceneId, String programSceneIds) {
        if(StringUtils.isEmpty(relIds) && StringUtils.isEmpty(programSceneIds)){
            throw new JeecgBootException("relIds 与 programSceneIds 不能都为空");
        }
        // 操作类型兼容：开启/关闭 或 OPEN/CLOSE
        String op = operationType;
        if("OPEN".equalsIgnoreCase(op)){
            op = LightingPlan.OPERATION_TYPE_OPEN;
        }else if("CLOSE".equalsIgnoreCase(op)){
            op = LightingPlan.OPERATION_TYPE_CLOSE;
        }
        if(!LightingPlan.OPERATION_TYPE_OPEN.equals(op) && !LightingPlan.OPERATION_TYPE_CLOSE.equals(op)){
            throw new JeecgBootException("operationType 必须为 开启/关闭 或 OPEN/CLOSE");
        }
        Set<Long> ids = new HashSet<>();
        if(StringUtils.isNotEmpty(relIds)){
            for(String s : relIds.split(",")){
                if(StringUtils.isBlank(s)){
                    continue;
                }
                try{
                    ids.add(Long.parseLong(s.trim()));
                }catch (NumberFormatException e){
                    throw new JeecgBootException("relIds 包含非法ID，无法解析: " + s);
                }
            }
            if(ids.isEmpty()){
                throw new JeecgBootException("relIds 不能为空");
            }
            if(!LightingPlan.REL_TYPE_AREA.equals(relType) && !LightingPlan.REL_TYPE_CIRCUIT.equals(relType)){
                throw new JeecgBootException("relType 必须为 区域 或 回路");
            }
        }
        // relType=区域 时按场景查询列表（复用给日志场景名、泛光判断、状态同步，避免重复查询）
        List<org.jeecg.modules.bems.lighting.entity.LightingScene> sceneList = null;
        if(LightingPlan.REL_TYPE_AREA.equals(relType) && CollectionUtil.isNotEmpty(ids)){
            sceneList = lightingSceneService.listByIds(ids);
        }
        // 解析场景名称（优先 sceneId：先在 sceneList 里匹配，未命中再单查；其次 relType=区域 时 relIds 即场景ID）
        String sceneName = null;
        if(sceneId != null){
            org.jeecg.modules.bems.lighting.entity.LightingScene sc = null;
            if(CollectionUtil.isNotEmpty(sceneList)){
                for(org.jeecg.modules.bems.lighting.entity.LightingScene s : sceneList){
                    if(sceneId.equals(s.getId())){
                        sc = s;
                        break;
                    }
                }
            }
            if(sc == null){
                sc = lightingSceneService.getById(sceneId);
            }
            if(sc != null){
                sceneName = sc.getSceneName();
            }
        }
        if(sceneName == null && CollectionUtil.isNotEmpty(sceneList)){
            sceneName = sceneList.get(0).getSceneName();
        }
        // 记录场景控制日志（父日志，操作类型=场景），子日志通过 parentId 自动继承
        LightingOperationLog planLog = new LightingOperationLog();
        planLog.setLogType(LightingOperationLog.LOG_TYPE_SCENE);
        planLog.setParentId(null);
        planLog.setRelType(relType);
        Long logRelId = sceneId;
        if(logRelId == null && !ids.isEmpty()){
            logRelId = ids.iterator().next();
        }
        if(logRelId == null && StringUtils.isNotEmpty(programSceneIds)){
            try {
                logRelId = Long.parseLong(programSceneIds.split(",")[0].trim());
            } catch (Exception ignored) {
                // 解析失败则日志不填 relId
            }
        }
        planLog.setRelId(logRelId);
        planLog.setName(sceneName != null ? sceneName : "场景控制");
        planLog.setOperationTime(java.time.LocalDateTime.now());
        planLog.setOperationType("场景" + op);
        String operationBy = "照明计划";
        try {
            org.jeecg.common.system.vo.LoginUser sysUser = (org.jeecg.common.system.vo.LoginUser) org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
            if (sysUser != null) {
                operationBy = sysUser.getUsername();
            }
        } catch (Exception e) {
            // 异步场景中SecurityManager不可用，使用默认用户
        }
        planLog.setOperationBy(operationBy);
        planLog.setOperatorType(LightingOperationLog.OPERATOR_TYPE_SCENE);
        lightingOperationLogService.save(planLog);

        if(LightingPlan.REL_TYPE_AREA.equals(relType)){
            // 区域（场景）批量全开/全关
            // 先查询场景列表，判断是否有泛光节目ID（上面已查询，sceneList 复用）

            // 引用了节目（programSceneIds，关联 lighting_program.id）的场景：节目由 executeProgramScenes 按 groupId 发泛光节目MQ，不走区域控制
            Set<Long> normalIds = new HashSet<>(ids); // 默认全部走原来的逻辑
            if(CollectionUtil.isNotEmpty(sceneList)){
                for(org.jeecg.modules.bems.lighting.entity.LightingScene scene : sceneList){
                    if(StringUtils.isNotEmpty(scene.getProgramSceneIds())){
                        // 从普通列表里移除，不走区域控制（节目走 executeProgramScenes）
                        normalIds.remove(scene.getId());
                    }
                }
            }

            // relIds 直接传了节目ID（纯节目场景没有区域/回路明细，relIds 即引用的节目ID）→ 按节目执行
            if(!normalIds.isEmpty()){
                List<LightingProgram> directPrograms = lightingProgramService.listByIds(normalIds);
                if(CollectionUtil.isNotEmpty(directPrograms)){
                    for(LightingProgram p : directPrograms){
                        normalIds.remove(p.getId());
                    }
                    executeProgramList(directPrograms, op, planLog.getId());
                }
            }
            // 走原来的逻辑（剩余为真实区域ID）
            if(!normalIds.isEmpty()){
                executeArea(normalIds, op, planLog.getId());
            }

            // 执行场景引用的节目（节目按 groupId 发泛光节目MQ，日志挂在父日志下）
            if(CollectionUtil.isNotEmpty(sceneList)){
                for(org.jeecg.modules.bems.lighting.entity.LightingScene scene : sceneList){
                    executeProgramScenes(scene.getProgramSceneIds(), op, planLog.getId(), 0);
                }
            }
        }else if(LightingPlan.REL_TYPE_CIRCUIT.equals(relType)){
            // 回路批量开启/关闭
            executeCircuit(ids, op, planLog.getId());
        }

        // 直接传 programSceneIds：只执行节目（无需 relType/relIds，按节目 groupId 发泛光节目MQ）
        if(StringUtils.isNotEmpty(programSceneIds)){
            executeProgramScenes(programSceneIds, op, planLog.getId(), 0);
        }

        // 控制成功后，同步更新关联场景明细的操作类型（scene/listPage 的 operationType 显示开启/关闭）
        // relType=区域 时复用上面已查出的 sceneList，避免重复查询
        syncSceneOperationType(relType, ids, op, sceneId,
                LightingPlan.REL_TYPE_AREA.equals(relType) ? sceneList : null);
    }

    /**
     * 按节目列表直接执行（relIds 直接传 lighting_program.id 的纯节目场景），
     * 按 groupId（泛光节目ID）发送MQ给小程序控制（onOff：1开2关），日志挂在父日志下。
     */
    private void executeProgramList(List<LightingProgram> programs, String op, Long parentLogId) {
        if(CollectionUtil.isEmpty(programs)){
            return;
        }
        for(LightingProgram program : programs){
            if(StringUtils.isNotBlank(program.getGroupId())){
                int onOff = LightingPlan.OPERATION_TYPE_OPEN.equals(op) ? 1 : 2;
                log.info("执行节目【{}】{}，发送泛光节目MQ：groupId={}", program.getProgramName(), op, program.getGroupId());
                lightingSendService.sendGroupOper(program.getGroupId(), onOff, program.getId(), program.getProgramName());
                buildProgramLog(program.getProgramName(), program.getId(), op, parentLogId);
            }else{
                log.warn("节目【{}】未配置泛光节目ID(groupId)，跳过", program.getProgramName());
            }
        }
    }

    /**
     * 执行场景引用的节目：programSceneIds 为 lighting_program.id 集合（节目表），
     * 节目不查区域/回路明细，而是按 groupId（泛光节目ID）发送MQ给小程序控制（onOff：1开2关）。
     */
    private void executeProgramScenes(String programSceneIds, String op, Long parentLogId, int depth) {
        if(StringUtils.isEmpty(programSceneIds)){
            return;
        }
        if(depth > 5){
            throw new JeecgBootException("节目嵌套层级过深，可能存在循环引用");
        }
        Set<Long> programIds = new HashSet<>();
        for(String s : programSceneIds.split(",")){
            if(StringUtils.isBlank(s.trim())){
                continue;
            }
            try{
                programIds.add(Long.parseLong(s.trim()));
            }catch (NumberFormatException e){
                log.warn("programSceneIds 包含非法ID，已忽略: {}", s);
            }
        }
        if(programIds.isEmpty()){
            return;
        }
        List<LightingProgram> programs = lightingProgramService.listByIds(programIds);
        if(programs.size() < programIds.size()){
            log.warn("场景引用节目 {} 个，实际找到 {} 个，缺失的ID已跳过", programIds.size(), programs.size());
        }
        for(LightingProgram program : programs){
            // 节目按 groupId（泛光节目ID）发 MQ 给小程序，不走区域/回路明细
            if(StringUtils.isNotBlank(program.getGroupId())){
                int onOff = LightingPlan.OPERATION_TYPE_OPEN.equals(op) ? 1 : 2;
                log.info("执行节目【{}】{}，发送泛光节目MQ：groupId={}", program.getProgramName(), op, program.getGroupId());
                lightingSendService.sendGroupOper(program.getGroupId(), onOff, program.getId(), program.getProgramName());
                // 记录节目控制日志（挂在父日志下）
                buildProgramLog(program.getProgramName(), program.getId(), op, parentLogId);
            }else{
                log.warn("节目【{}】未配置泛光节目ID(groupId)，跳过", program.getProgramName());
            }
        }
    }

    /**
     * 记录节目控制子日志（logType=节目、operatorType=场景、name=节目名、operationType=节目+开/关），
     * 挂在场景控制父日志（parentId）下，与区域/回路子日志同一层级。
     */
    private void buildProgramLog(String sceneName, Long sceneId, String op, Long parentLogId) {
        LightingOperationLog programLog = new LightingOperationLog();
        programLog.setLogType(LightingOperationLog.LOG_TYPE_PROGRAM);
        programLog.setParentId(parentLogId);
        programLog.setRelType("场景");
        programLog.setRelId(sceneId);
        programLog.setName(sceneName);
        programLog.setOperationTime(java.time.LocalDateTime.now());
        programLog.setOperationType("节目" + op);
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
        lightingOperationLogService.save(programLog);
    }

    /**
     * 控制成功后同步场景明细的操作类型：
     * 1. 前端传了 sceneId：只更新该场景（精准）；
     * 2. 未传 sceneId 且 relType=区域 时，relIds 本身可能是场景ID（引用了节目的场景），直接命中（复用已查询的 scenes）；
     * 3. 仍未命中则反查 lighting_scene_detail，把明细中包含这些控制目标（relType+relId）的场景全部更新。
     * 操作类型值：开启/关闭，保证 scene/listPage 返回的 operationType 与最后一次控制一致。
     * （lighting_scene.status 保持 启用/禁用 语义，不做开关状态）
     */
    private void syncSceneOperationType(String relType, Set<Long> ids, String op, Long sceneId,
                                        List<org.jeecg.modules.bems.lighting.entity.LightingScene> scenes) {
        try {
            Set<Long> sceneIds = new HashSet<>();
            // 1. 前端指定场景：精准更新，不做反查
            if(sceneId != null){
                sceneIds.add(sceneId);
            }else{
                // 2. relType=区域 时，relIds 可能本身就是场景ID（引用了节目的场景），直接命中
                if(LightingPlan.REL_TYPE_AREA.equals(relType) && CollectionUtil.isNotEmpty(scenes)){
                    scenes.forEach(s -> sceneIds.add(s.getId()));
                }
                // 3. 反查明细：场景明细中包含这些目标（区域ID或回路ID）的场景
                if(sceneIds.isEmpty()){
                    List<org.jeecg.modules.bems.lighting.entity.LightingSceneDetail> details = lightingSceneDetailMapper.selectList(
                            new LambdaQueryWrapper<org.jeecg.modules.bems.lighting.entity.LightingSceneDetail>()
                                    .eq(org.jeecg.modules.bems.lighting.entity.LightingSceneDetail::getRelType, relType)
                                    .in(org.jeecg.modules.bems.lighting.entity.LightingSceneDetail::getRelId, ids));
                    if(CollectionUtil.isNotEmpty(details)){
                        details.forEach(d -> sceneIds.add(d.getSceneId()));
                    }
                }
            }
            if(sceneIds.isEmpty()){
                return;
            }
            // 批量更新场景明细的操作类型
            org.jeecg.modules.bems.lighting.entity.LightingSceneDetail update = new org.jeecg.modules.bems.lighting.entity.LightingSceneDetail();
            update.setOperationType(op);
            lightingSceneDetailMapper.update(update, new LambdaQueryWrapper<org.jeecg.modules.bems.lighting.entity.LightingSceneDetail>()
                    .in(org.jeecg.modules.bems.lighting.entity.LightingSceneDetail::getSceneId, sceneIds));
            log.info("控制成功，同步场景明细操作类型为【{}】，场景ID：{}", op, sceneIds);
        } catch (Exception e) {
            // 操作类型同步失败不影响控制结果，只记录日志
            log.error("同步场景明细操作类型失败：{}", e.getMessage(), e);
        }
    }
}
