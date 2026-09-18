package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingTimerTaskDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.entity.LightingScene;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecutionTimeService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.jeecg.modules.bems.lighting.service.ILightingSceneService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 定时控制 - 定时任务管理
 */
@Api(tags = "定时控制 - 定时任务管理")
@Slf4j
@RestController
@RequestMapping("/bems/lighting/timerTask")
@AllArgsConstructor
public class LightingTimerTaskController {

    private final ILightingPlanService planService;

    private final ILightingPlanExecutionTimeService executionTimeService;

    private final ILightingAreaService areaService;

    private final ILightingCircuitService circuitService;

    private final ILightingSceneService sceneService;

    /**
     * 定时任务列表（分页）
     */
    @ApiOperation("定时任务列表（分页）")
    @GetMapping("/listPage")
    public Result<IPage<LightingTimerTaskDto>> listPage(LightingTimerTaskDto query) {
        Page<LightingPlan> page = planService.page(
                new Page<>(query.getPageNo(), query.getPageSize()),
                new LambdaQueryWrapper<LightingPlan>()
                        .like(StringUtils.isNotEmpty(query.getPlanName()), LightingPlan::getPlanName, query.getPlanName())
                        .eq(StringUtils.isNotEmpty(query.getStatus()), LightingPlan::getStatus, query.getStatus())
                        .eq(StringUtils.isNotEmpty(query.getPlanType()), LightingPlan::getPlanType, query.getPlanType())
                        .orderByDesc(LightingPlan::getId)
        );

        List<LightingPlan> plans = page.getRecords();
        if (plans.isEmpty()) {
            Page<LightingTimerTaskDto> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            result.setRecords(Collections.emptyList());
            return Result.ok(result);
        }

        List<Long> planIds = plans.stream().map(LightingPlan::getId).toList();
        Map<Long, LightingPlanExecutionTime> executionTimeMap = executionTimeService.getByPlanIds(planIds)
                .stream()
                .collect(Collectors.toMap(LightingPlanExecutionTime::getPlanId, Function.identity(), (a, b) -> a));

        Map<Long, String> relNameMap = buildRelNameMap(plans);

        List<LightingTimerTaskDto> records = plans.stream().map(plan -> {
            LightingTimerTaskDto dto = new LightingTimerTaskDto();
            dto.setId(plan.getId());
            dto.setPlanName(plan.getPlanName());
            dto.setRelType(plan.getRelType());
            dto.setRelIds(plan.getRelIds());
            dto.setRelNames(relNameMap.getOrDefault(plan.getId(), ""));
            dto.setPlanType(plan.getPlanType());
            dto.setCycleType(plan.getCycleType());
            dto.setOperationType(plan.getOperationType());
            dto.setStatus(plan.getStatus());
            dto.setExecutionTime(plan.getExecutionTime());

            LightingPlanExecutionTime et = executionTimeMap.get(plan.getId());
            if (et != null) {
                dto.setExecutionTime(et.getExecutionTime());
                dto.setStartDate(et.getStartDate());
                dto.setEndDate(et.getEndDate());
                dto.setEnabledWeek(et.getEnabledWeek());
                dto.setVersion(et.getVersion());
            }
            return dto;
        }).toList();

        Page<LightingTimerTaskDto> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return Result.ok(result);
    }

    /**
     * 新增定时任务
     */
    @ApiOperation("新增定时任务")
    @PostMapping("/add")
    public Result<String> add(@RequestBody LightingTimerTaskDto dto) {
        LightingPlan plan = new LightingPlan();
        plan.setPlanName(dto.getPlanName());
        plan.setRelType(dto.getRelType());
        plan.setRelIds(dto.getRelIds());
        plan.setOperationType(dto.getOperationType());
        plan.setPlanType(dto.getPlanType() != null ? dto.getPlanType() : "定时任务");
        plan.setCycleType(dto.getCycleType() != null ? dto.getCycleType() : "自定义");
        plan.setExecutionTime(dto.getExecutionTime());
        plan.setStatus(LightingPlan.STATUS_DISABLE);

        planService.add(plan);

        if (dto.getExecutionTime() != null) {
            LightingPlanExecutionTime et = new LightingPlanExecutionTime();
            et.setPlanId(plan.getId());
            et.setExecutionTime(dto.getExecutionTime());
            et.setStartDate(dto.getStartDate());
            et.setEndDate(dto.getEndDate());
            et.setEnabledWeek(dto.getEnabledWeek());
            et.setVersion(UUID.randomUUID().toString());
            executionTimeService.saveOrUpdate(et);
        }

        // 配置操作日志由 planService.add 统一记录（/plan 与 /timerTask 两套接口共用同一口径，避免重复记账）

        return Result.ok("新增成功");
    }

    /**
     * 编辑定时任务
     */
    @ApiOperation("编辑定时任务")
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody LightingTimerTaskDto dto) {
        LightingPlan plan = planService.getById(dto.getId());
        if (plan == null) {
            return Result.error("任务不存在");
        }
        if (LightingPlan.STATUS_ENABLE.equals(plan.getStatus())) {
            return Result.error("任务已启用，无法编辑");
        }

        // 执行时间配置（下面 plan 会被就地覆盖，需先取出旧记录用于增量更新）
        LightingPlanExecutionTime oldEt = executionTimeService.getByPlanId(plan.getId());

        plan.setPlanName(dto.getPlanName());
        plan.setRelType(dto.getRelType());
        plan.setRelIds(dto.getRelIds());
        plan.setOperationType(dto.getOperationType());
        plan.setPlanType(dto.getPlanType());
        plan.setCycleType(dto.getCycleType());
        plan.setExecutionTime(dto.getExecutionTime());

        planService.edit(plan);

        LightingPlanExecutionTime et = oldEt;
        if (et == null) {
            et = new LightingPlanExecutionTime();
            et.setPlanId(plan.getId());
        }
        et.setExecutionTime(dto.getExecutionTime());
        et.setStartDate(dto.getStartDate());
        et.setEndDate(dto.getEndDate());
        et.setEnabledWeek(dto.getEnabledWeek());
        et.setVersion(UUID.randomUUID().toString());
        executionTimeService.saveOrUpdate(et);

        // 配置操作日志由 planService.edit 统一记录（旧值 → 新值差异）

        return Result.ok("编辑成功");
    }

    /**
     * 启用定时任务
     */
    @ApiOperation("启用定时任务")
    @PostMapping("/enable")
    public Result<String> enable(@RequestParam Long id) {
        LightingPlan plan = planService.getById(id);
        if (plan == null) {
            return Result.error("任务不存在");
        }
        LightingPlanExecutionTime et = executionTimeService.getByPlanId(id);
        if (et == null) {
            return Result.error("请先设置执行时间");
        }
        // 配置操作日志由 planService.enable 统一记录（scheduleJobId 不为空时服务内部直接 return，不会记账）
        planService.enable(et);
        return Result.ok("启用成功");
    }

    /**
     * 停用定时任务
     */
    @ApiOperation("停用定时任务")
    @PostMapping("/disable")
    public Result<String> disable(@RequestParam Long id) {
        // 配置操作日志由 planService.disable 统一记录（已停用、由调度器接管的不记）
        planService.disable(id);
        return Result.ok("停用成功");
    }

    /**
     * 删除定时任务
     */
    @ApiOperation("删除定时任务")
    @PostMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        // 配置操作日志由 planService.delete 统一记录；执行时间/执行日志也由服务内一并清理
        planService.delete(id);
        return Result.ok("删除成功");
    }

    /**
     * 定时任务详情
     */
    @ApiOperation("定时任务详情")
    @GetMapping("/detail")
    public Result<LightingTimerTaskDto> detail(@RequestParam Long id) {
        LightingPlan plan = planService.getById(id);
        if (plan == null) {
            return Result.error("任务不存在");
        }
        LightingPlanExecutionTime et = executionTimeService.getByPlanId(id);
        LightingTimerTaskDto dto = new LightingTimerTaskDto();
        dto.setId(plan.getId());
        dto.setPlanName(plan.getPlanName());
        dto.setRelType(plan.getRelType());
        dto.setRelIds(plan.getRelIds());
        dto.setRelNames(buildRelNameMap(List.of(plan)).getOrDefault(plan.getId(), ""));
        dto.setPlanType(plan.getPlanType());
        dto.setCycleType(plan.getCycleType());
        dto.setOperationType(plan.getOperationType());
        dto.setStatus(plan.getStatus());
        dto.setExecutionTime(plan.getExecutionTime());

        if (et != null) {
            dto.setExecutionTime(et.getExecutionTime());
            dto.setStartDate(et.getStartDate());
            dto.setEndDate(et.getEndDate());
            dto.setEnabledWeek(et.getEnabledWeek());
            dto.setVersion(et.getVersion());
        }
        return Result.ok(dto);
    }

    private Map<Long, String> buildRelNameMap(List<LightingPlan> plans) {
        Map<Long, String> result = new HashMap<>();
        for (LightingPlan plan : plans) {
            if (StringUtils.isEmpty(plan.getRelIds())) {
                continue;
            }
            List<Long> relIds = Arrays.stream(plan.getRelIds().split(","))
                    .map(Long::parseLong).toList();
            if (LightingPlan.REL_TYPE_AREA.equals(plan.getRelType())) {
                List<LightingArea> areas = areaService.listByIds(relIds);
                String names = areas.stream().map(LightingArea::getAreaName)
                        .collect(Collectors.joining("、"));
                result.put(plan.getId(), names);
            } else if (LightingPlan.REL_TYPE_CIRCUIT.equals(plan.getRelType())) {
                List<LightingCircuit> circuits = circuitService.listByIds(relIds);
                String names = circuits.stream().map(c -> {
                    StringBuilder sb = new StringBuilder();
                    if (c.getAreaCode() != null) {
                        sb.append(c.getAreaCode()).append("-");
                    }
                    sb.append(c.getCircuitName());
                    return sb.toString();
                }).collect(Collectors.joining("、"));
                result.put(plan.getId(), names);
            } else if (LightingPlan.REL_TYPE_SCENE.equals(plan.getRelType())) {
                List<LightingScene> scenes = sceneService.listByIds(relIds);
                String names = scenes.stream().map(LightingScene::getSceneName)
                        .collect(Collectors.joining("、"));
                result.put(plan.getId(), names);
            } else {
                result.put(plan.getId(), plan.getRelIds());
            }
        }
        return result;
    }

    // 配置日志文案（快照/差异/星期与窗口展示）已抽到 LightingPlanLogText，
    // 与 /bems/lighting/plan 那套接口共用同一份实现，此处不再保留副本
}
