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
import org.jeecg.modules.bems.lighting.service.ILightingConfigLogService;
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
     * 配置操作日志（新增/修改/启停/删除定时任务时留痕）
     */
    private final ILightingConfigLogService configLogService;

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

        // 配置操作日志
        configLogService.saveLog("新增", "定时控制", "定时任务", plan.getId(), plan.getPlanName(),
                "新增定时任务；" + describeTask(plan, plan.getExecutionTime(),
                        dto.getStartDate(), dto.getEndDate(), dto.getEnabledWeek()));

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

        // 修改前快照（下面 plan 会被就地覆盖，需先取旧值用于对比）
        LightingPlanExecutionTime oldEt = executionTimeService.getByPlanId(plan.getId());
        Map<String, String> before = taskSnapshot(plan, plan.getExecutionTime(),
                oldEt == null ? null : oldEt.getStartDate(),
                oldEt == null ? null : oldEt.getEndDate(),
                oldEt == null ? null : oldEt.getEnabledWeek());

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

        // 配置操作日志（记录"旧值 → 新值"差异）
        Map<String, String> after = taskSnapshot(plan, plan.getExecutionTime(),
                dto.getStartDate(), dto.getEndDate(), dto.getEnabledWeek());
        configLogService.saveLog("修改", "定时控制", "定时任务", plan.getId(), plan.getPlanName(),
                diffSnapshot(before, after));

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
        planService.enable(et);
        // 配置操作日志（scheduleJobId 不为空时由动态调度器接管，本计划未实际启用，不记日志）
        if (plan.getScheduleJobId() == null) {
            configLogService.saveLog("修改", "定时控制", "定时任务", plan.getId(), plan.getPlanName(),
                    "启用定时任务；" + describeTask(plan, et.getExecutionTime(),
                            et.getStartDate(), et.getEndDate(), et.getEnabledWeek()));
        }
        return Result.ok("启用成功");
    }

    /**
     * 停用定时任务
     */
    @ApiOperation("停用定时任务")
    @PostMapping("/disable")
    public Result<String> disable(@RequestParam Long id) {
        LightingPlan plan = planService.getById(id);
        planService.disable(id);
        // 配置操作日志（仅记录"确实由启用改为停用"：已停用、由调度器接管的不记）
        if (plan != null && plan.getScheduleJobId() == null
                && LightingPlan.STATUS_ENABLE.equals(plan.getStatus())) {
            configLogService.saveLog("修改", "定时控制", "定时任务", plan.getId(), plan.getPlanName(), "停用定时任务");
        }
        return Result.ok("停用成功");
    }

    /**
     * 删除定时任务
     */
    @ApiOperation("删除定时任务")
    @PostMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        // 删除前取出计划与执行时间配置（删除后无法再查），用于记录操作日志
        LightingPlan plan = planService.getById(id);
        LightingPlanExecutionTime et = executionTimeService.getByPlanId(id);
        executionTimeService.remove(
                new LambdaQueryWrapper<LightingPlanExecutionTime>()
                        .eq(LightingPlanExecutionTime::getPlanId, id)
        );
        planService.delete(id);
        // 配置操作日志
        if (plan != null) {
            configLogService.saveLog("删除", "定时控制", "定时任务", id, plan.getPlanName(),
                    "删除定时任务；" + describeTask(plan, plan.getExecutionTime(),
                            et == null ? null : et.getStartDate(),
                            et == null ? null : et.getEndDate(),
                            et == null ? null : et.getEnabledWeek()));
        }
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

    // ==================== 配置操作日志：内容拼装 ====================

    private static final String[] WEEK_NAMES = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    /**
     * 定时任务快照（用于对比"改了哪些字段"）。
     * 不含状态字段：编辑时不允许已启用，启停动作由 /enable、/disable 单独记日志，
     * 避免出现"状态：禁用 → 禁用"这类噪音。
     */
    private Map<String, String> taskSnapshot(LightingPlan plan, String executionTime,
                                             String startDate, String endDate, String enabledWeek) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("名称", plan.getPlanName());
        snapshot.put("计划类型", plan.getPlanType());
        snapshot.put("周期类型", plan.getCycleType());
        snapshot.put("目标", targetText(plan));
        snapshot.put("动作", plan.getOperationType());
        snapshot.put("执行时间", executionTime);
        snapshot.put("生效窗口", formatWindow(startDate, endDate));
        snapshot.put("执行星期", formatWeek(enabledWeek));
        return snapshot;
    }

    /**
     * 快照压缩为一行文本（新增/启用时直接作为操作内容），如：
     * 计划类型：定时任务；周期类型：自定义；目标：场景 服贸会全部；动作：开启；执行时间：19:00:00；…
     */
    private String describeTask(LightingPlan plan, String executionTime,
                                String startDate, String endDate, String enabledWeek) {
        return taskSnapshot(plan, executionTime, startDate, endDate, enabledWeek).entrySet().stream()
                .map(e -> e.getKey() + "：" + display(e.getValue()))
                .collect(Collectors.joining("；"));
    }

    /**
     * 快照差异（旧值 → 新值），无变化返回"无字段变更"
     */
    private String diffSnapshot(Map<String, String> before, Map<String, String> after) {
        List<String> changes = new ArrayList<>();
        before.forEach((label, oldValue) -> {
            String newValue = after.get(label);
            if (!Objects.equals(oldValue, newValue)) {
                changes.add(label + "：" + display(oldValue) + " → " + display(newValue));
            }
        });
        return changes.isEmpty() ? "无字段变更" : String.join("；", changes);
    }

    /**
     * 控制目标描述：场景 服贸会全部（按 relType 反查区域/回路/场景名称）
     */
    private String targetText(LightingPlan plan) {
        String names = null;
        try {
            names = buildRelNameMap(List.of(plan)).get(plan.getId());
        } catch (Exception e) {
            log.warn("解析定时任务目标名称失败，计划id：{}，relIds：{}", plan.getId(), plan.getRelIds(), e);
        }
        if (StringUtils.isEmpty(names)) {
            names = plan.getRelIds();
        }
        return nullToEmpty(plan.getRelType()) + " " + nullToEmpty(names);
    }

    /**
     * 执行星期展示：1,2,3 → 周一、周二、周三；七天全选 → 每天；空 → 不限
     */
    private String formatWeek(String enabledWeek) {
        if (StringUtils.isBlank(enabledWeek)) {
            return "不限";
        }
        Set<Integer> days = new LinkedHashSet<>();
        for (String s : enabledWeek.split(",")) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            try {
                days.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException e) {
                // 非数字（如直接存"每天"）忽略，最后原样返回
            }
        }
        if (days.isEmpty()) {
            return enabledWeek;
        }
        if (days.size() == 7) {
            return "每天";
        }
        return days.stream().filter(d -> d >= 1 && d <= 7).map(d -> WEEK_NAMES[d])
                .collect(Collectors.joining("、"));
    }

    /**
     * 生效窗口展示：起止同一天显示单日，都为空显示不限
     */
    private String formatWindow(String startDate, String endDate) {
        if (StringUtils.isBlank(startDate) && StringUtils.isBlank(endDate)) {
            return "不限";
        }
        if (Objects.equals(startDate, endDate)) {
            return nullToEmpty(startDate);
        }
        return nullToEmpty(startDate) + "~" + nullToEmpty(endDate);
    }

    private String display(String value) {
        return StringUtils.isBlank(value) ? "空" : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
