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
import org.jeecg.modules.bems.lighting.dto.LightingPlanDetailDto;
import org.jeecg.modules.bems.lighting.dto.LightingPlanQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecuteLog;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.mapper.LightingPlanMapper;
import org.jeecg.modules.bems.lighting.mapper.LightingSceneDetailMapper;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.jeecg.modules.bems.lighting.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
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

        // 记录定时任务操作日志（父日志）
        LightingOperationLog planLog = new LightingOperationLog();
        planLog.setLogType(LightingOperationLog.LOG_TYPE_PLAN);
        planLog.setParentId(null);
        planLog.setRelType("定时任务");
        planLog.setRelId(id);
        planLog.setName(plan.getPlanName());
        planLog.setOperationTime(java.time.LocalDateTime.now());
        planLog.setOperationType("定时任务" + plan.getOperationType());
        planLog.setOperationBy("照明计划");
        planLog.setOperatorType(LightingOperationLog.OPERATOR_TYPE_PLAN);
        lightingOperationLogService.save(planLog);

        Set<Long> relIds = Arrays.stream(plan.getRelIds().split(",")).map(Long::parseLong).collect(Collectors.toSet());
        if(LightingPlan.REL_TYPE_AREA.equals(plan.getRelType())){
            // 区域（场景）
            executeArea(relIds, plan.getOperationType(), planLog.getId());
        }else if(LightingPlan.REL_TYPE_CIRCUIT.equals(plan.getRelType())){
            // 回路
            executeCircuit(relIds, plan.getOperationType(), planLog.getId());
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
            }
        }

        return dto;
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

        // 记录定时任务操作日志（父日志）
        LightingOperationLog planLog = new LightingOperationLog();
        planLog.setLogType(LightingOperationLog.LOG_TYPE_PLAN);
        planLog.setParentId(null);
        planLog.setRelType("定时任务");
        planLog.setRelId(id);
        planLog.setName(plan.getPlanName());
        planLog.setOperationTime(java.time.LocalDateTime.now());
        planLog.setOperationType("定时任务" + plan.getOperationType());
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
            // 区域（场景）
            executeArea(relIds, plan.getOperationType(), planLog.getId());
        }else if(LightingPlan.REL_TYPE_CIRCUIT.equals(plan.getRelType())){
            // 回路
            executeCircuit(relIds, plan.getOperationType(), planLog.getId());
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
    public void control(String relType, String relIds, String operationType, Long sceneId) {
        if(StringUtils.isEmpty(relIds)){
            throw new JeecgBootException("relIds 不能为空");
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
        // relType=区域 时按场景查询列表（复用给日志场景名、泛光判断、状态同步，避免重复查询）
        List<org.jeecg.modules.bems.lighting.entity.LightingScene> sceneList = null;
        if(LightingPlan.REL_TYPE_AREA.equals(relType)){
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
        planLog.setRelId(sceneId != null ? sceneId : ids.iterator().next());
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

            // 操作类型转成泛光的 onOff：1开，2关
            int onOff = LightingPlan.OPERATION_TYPE_OPEN.equals(op) ? 1 : 2;

            // 有泛光节目ID的走MQ，没有的（包括查不到场景的）走原来的逻辑
            Set<Long> normalIds = new HashSet<>(ids); // 默认全部走原来的逻辑
            if(CollectionUtil.isNotEmpty(sceneList)){
                for(org.jeecg.modules.bems.lighting.entity.LightingScene scene : sceneList){
                    if(StringUtils.isNotBlank(scene.getGroupId())){
                        // 有泛光节目ID，发MQ给小程序调节目
                        lightingSendService.sendGroupOper(scene.getGroupId(), onOff, scene.getId(), scene.getSceneName());
                        // 从普通列表里移除，不走原来的逻辑
                        normalIds.remove(scene.getId());
                    }
                }
            }

            // 走原来的逻辑
            if(!normalIds.isEmpty()){
                executeArea(normalIds, op, planLog.getId());
            }
        }else if(LightingPlan.REL_TYPE_CIRCUIT.equals(relType)){
            // 回路批量开启/关闭
            executeCircuit(ids, op, planLog.getId());
        }

        // 控制成功后，同步更新关联场景明细的操作类型（scene/listPage 的 operationType 显示开启/关闭）
        // relType=区域 时复用上面已查出的 sceneList，避免重复查询
        syncSceneOperationType(relType, ids, op, sceneId,
                LightingPlan.REL_TYPE_AREA.equals(relType) ? sceneList : null);
    }

    /**
     * 控制成功后同步场景明细的操作类型：
     * 1. 前端传了 sceneId：只更新该场景（精准）；
     * 2. 未传 sceneId 且 relType=区域 时，relIds 本身可能是场景ID（泛光节目场景），直接命中（复用已查询的 scenes）；
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
                // 2. relType=区域 时，relIds 可能本身就是场景ID（泛光节目场景），直接命中
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
