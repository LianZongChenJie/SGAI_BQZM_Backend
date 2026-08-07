package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecuteLog;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecuteLogService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecutionTimeService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 控制日历
 *
 * 显示指定月份内照明计划的执行情况（按日期）：
 * 数据源与 /bems/lighting/plan/listPage 一致 —— 仅查 lighting_plan 照明计划表，
 * 且只展示 status=启用 的计划（含定时任务同步出的计划）。
 *
 * 事件状态（status）判定：
 * - 未来日期（执行时间未到）→ 待执行
 * - 执行时间已过，按 MQ 执行日志（lighting_plan_execute_log）判定：
 *   - 日志=执行成功 → 执行成功（MQ 消息已被消费）
 *   - 日志=执行失败 / 待消费（已发送但未被消费）→ 执行失败
 * - 无执行日志（历史数据）时，按当天操作日志兜底：
 *   - 当天有计划/定时器执行日志 → 执行成功
 *   - 否则 → 待执行（未执行）
 *
 * 注：执行日志不作为独立事件展示在日历上，通过详情接口 /detail 按日期查询。
 */
@Api(tags = "照明-控制日历")
@Slf4j
@RestController
@RequestMapping("/bems/lighting/calendar")
@AllArgsConstructor
public class LightingCalendarController {

    /** 事件状态：待执行 */
    private static final String STATUS_PENDING = "待执行";
    /** 事件状态：执行成功（MQ 消息已被消费） */
    private static final String STATUS_SUCCESS = "执行成功";
    /** 事件状态：执行失败（MQ 消息未被消费/消费异常/执行失败） */
    private static final String STATUS_FAIL = "执行失败";

    /** 定时器触发执行的日志操作人 */
    private static final String OPERATOR_TIMER = "定时器";
    /** 照明计划触发执行的日志操作人（默认值） */
    private static final String OPERATOR_PLAN = "照明计划";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ILightingPlanService planService;
    private final ILightingPlanExecutionTimeService executionTimeService;
    private final ILightingOperationLogService lightingOperationLogService;
    private final ILightingPlanExecuteLogService lightingPlanExecuteLogService;
    private final ILightingAreaService lightingAreaService;
    private final ILightingCircuitService lightingCircuitService;

    /**
     * 查询指定年月的日历事件（照明计划的执行情况，含待执行/执行成功/执行失败状态）
     * 数据源与 /bems/lighting/plan/listPage 一致：仅查 lighting_plan 启用状态的计划
     * @param year 年份
     * @param month 月份 1-12
     */
    @ApiOperation("查询指定年月的日历事件（照明计划的执行情况，含待执行/执行成功/执行失败状态）")
    @GetMapping("/events")
    public Result<List<CalendarDayEvent>> events(@RequestParam int year, @RequestParam int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        LocalDateTime monthStartTime = monthStart.atStartOfDay();
        LocalDateTime monthEndTime = monthEnd.atTime(23, 59, 59);

        Map<String, List<CalendarEvent>> dayEventMap = new HashMap<>();

        // 1. 查询当月控制日志，构建“已执行”映射（供计划判断状态兜底），不作为独立事件展示
        List<LightingOperationLog> logs = loadMonthLogs(monthStartTime, monthEndTime);
        Map<String, Set<Long>> executedRelIdsByDate = buildExecutedRelIdsByDate(logs);

        // 2. 查询当月 MQ 执行日志（日期 → 计划ID → 状态），用于 执行成功/执行失败 判定
        Map<String, Map<Long, String>> executeLogStatusByDate = loadMonthExecuteLogs(monthStart, monthEnd);

        // 3. 照明计划事件（与 plan/listPage 同源，仅启用状态）
        loadPlanEvents(monthStart, monthEnd, dayEventMap, executedRelIdsByDate, executeLogStatusByDate);

        // 组装返回结果
        List<CalendarDayEvent> result = new ArrayList<>();
        for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
            String dateKey = date.format(DATE_FMT);
            CalendarDayEvent dayEvent = new CalendarDayEvent();
            dayEvent.setDate(dateKey);
            dayEvent.setDayOfWeek(String.valueOf(date.getDayOfWeek().getValue()));
            dayEvent.setEvents(dayEventMap.getOrDefault(dateKey, Collections.emptyList()));
            result.add(dayEvent);
        }

        return Result.ok(result);
    }

    /**
     * 查询日历事件详情（点击日历事件后调用）：
     * - 当天有计划/定时器执行日志 → 返回执行日志列表（logs）
     * - 当天无执行日志 → 返回要执行的区域/回路目标列表（targets）
     *
     * @param source 来源：PLAN-照明计划、LOG-历史日志
     * @param planId 关联ID（计划ID；LOG 来源时为日志的目标ID）
     * @param date 日期 yyyy-MM-dd
     */
    @ApiOperation("查询日历事件详情（执行日志或要执行的区域/回路）")
    @GetMapping("/detail")
    public Result<CalendarEventDetail> detail(@RequestParam String source,
                                              @RequestParam Long planId,
                                              @RequestParam String date) {
        CalendarEventDetail detail = new CalendarEventDetail();
        detail.setSource(source);
        detail.setPlanId(planId);
        detail.setDate(date);

        if ("LOG".equals(source)) {
            // 历史日志事件：planId 即日志的 relId，直接查询当天该目标的日志
            List<LightingOperationLog> logs = queryLogsByDateAndRelIds(date, Collections.singleton(planId));
            detail.setLogs(logs);
            detail.setStatus(logs.isEmpty() ? STATUS_PENDING : STATUS_SUCCESS);
            if (!logs.isEmpty()) {
                LightingOperationLog first = logs.get(0);
                detail.setPlanName(first.getName());
                detail.setRelType(first.getRelType());
                detail.setOperationType(first.getOperationType());
            }
            return Result.ok(detail);
        }

        if ("PLAN".equals(source)) {
            LightingPlan plan = planService.getById(planId);
            if (plan == null) {
                throw new JeecgBootException("计划不存在");
            }
            detail.setPlanName(plan.getPlanName());
            detail.setRelType(plan.getRelType());
            detail.setOperationType(plan.getOperationType());
            // 执行时间取执行配置表，与日历列表口径一致
            LightingPlanExecutionTime et = executionTimeService.getByPlanId(planId);
            String execTimeStr = et != null ? et.getExecutionTime() : plan.getExecutionTime();
            detail.setExecutionTime(execTimeStr);

            Set<Long> relIds = parseRelIds(plan.getRelIds());

            // 查询当天执行日志
            List<LightingOperationLog> logs = queryLogsByDateAndRelIds(date, relIds);
            detail.setLogs(logs);

            // 状态：未来时间 → 待执行；优先按 MQ 执行日志判定 执行成功/执行失败，无记录时按操作日志兜底
            String executeStatus = lookupExecuteLogStatus(date, planId);
            detail.setStatus(resolveDetailStatus(date, execTimeStr, executeStatus, logs));

            // 无执行日志时，返回要执行的区域/回路
            if (logs.isEmpty() && !relIds.isEmpty()) {
                detail.setTargets(buildTargets(plan.getRelType(), relIds));
            }
            return Result.ok(detail);
        }

        throw new JeecgBootException("source 必须为 PLAN/LOG");
    }

    /**
     * 查询指定日期内、目标ID集合上的执行日志（计划/定时器触发的控制记录）
     */
    private List<LightingOperationLog> queryLogsByDateAndRelIds(String date, Set<Long> relIds) {
        if (relIds == null || relIds.isEmpty()) {
            return Collections.emptyList();
        }
        LocalDate day;
        try {
            day = LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            throw new JeecgBootException("date 格式必须为 yyyy-MM-dd");
        }
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay().minusNanos(1);
        return lightingOperationLogService.list(
                new LambdaQueryWrapper<LightingOperationLog>()
                        .ge(LightingOperationLog::getOperationTime, start)
                        .le(LightingOperationLog::getOperationTime, end)
                        .in(LightingOperationLog::getRelId, relIds)
                        .orderByDesc(LightingOperationLog::getOperationTime));
    }


    /**
     * 构建要执行的区域/回路目标列表
     */
    private List<CalendarTarget> buildTargets(String relType, Set<Long> relIds) {
        List<CalendarTarget> targets = new ArrayList<>();
        if (relIds == null || relIds.isEmpty()) {
            return targets;
        }
        if ("区域".equals(relType)) {
            List<LightingArea> areas = lightingAreaService.getByIds(relIds);
            if (areas != null) {
                for (LightingArea area : areas) {
                    CalendarTarget target = new CalendarTarget();
                    target.setRelId(area.getId());
                    target.setRelName(area.getAreaName());
                    target.setRelType("区域");
                    targets.add(target);
                }
            }
        } else if ("回路".equals(relType)) {
            List<LightingCircuit> circuits = lightingCircuitService.listByIds(relIds);
            if (circuits != null) {
                for (LightingCircuit circuit : circuits) {
                    CalendarTarget target = new CalendarTarget();
                    target.setRelId(circuit.getId());
                    target.setRelName(circuit.getCircuitName());
                    target.setRelType("回路");
                    target.setElectricCurrent(circuit.getElectricCurrent());
                    targets.add(target);
                }
            }
        }
        return targets;
    }

    // ==================== 照明计划（与 /bems/lighting/plan/listPage 同源） ====================

    private void loadPlanEvents(LocalDate monthStart, LocalDate monthEnd,
                                 Map<String, List<CalendarEvent>> dayEventMap,
                                 Map<String, Set<Long>> executedRelIdsByDate,
                                 Map<String, Map<Long, String>> executeLogStatusByDate) {
        List<LightingPlan> enabledPlans = planService.list(
                new LambdaQueryWrapper<LightingPlan>()
                        .eq(LightingPlan::getStatus, LightingPlan.STATUS_ENABLE)
                        .orderByAsc(LightingPlan::getSort)
        );

        if (enabledPlans.isEmpty()) return;

        Map<Long, LightingPlanExecutionTime> executionTimeMap = executionTimeService.getByPlanIds(
                enabledPlans.stream().map(LightingPlan::getId).toList()
        ).stream().collect(Collectors.toMap(LightingPlanExecutionTime::getPlanId, et -> et, (a, b) -> a));

        for (LightingPlan plan : enabledPlans) {
            LightingPlanExecutionTime et = executionTimeMap.get(plan.getId());
            if (et == null) continue;

            LocalDate startDate, endDate;
            try {
                startDate = LocalDate.parse(et.getStartDate(), DATE_FMT);
                endDate = LocalDate.parse(et.getEndDate(), DATE_FMT);
            } catch (Exception e) {
                log.warn("解析计划时间配置失败, planId={}", plan.getId());
                continue;
            }

            if (startDate.isAfter(monthEnd) || endDate.isBefore(monthStart)) continue;

            Set<String> enabledWeekDays = new HashSet<>();
            if (et.getEnabledWeek() != null) {
                enabledWeekDays.addAll(Arrays.asList(et.getEnabledWeek().split(",")));
            }

            String operationLabel = LightingPlan.OPERATION_TYPE_OPEN.equals(plan.getOperationType()) ? "开灯" : "关灯";
            String timeStr = et.getExecutionTime() != null ? et.getExecutionTime() : "";
            String color = plan.getPlanType() != null && plan.getPlanType().contains("节日") ? "green" : "blue";
            LocalTime execTime = null;
            try {
                execTime = LocalTime.parse(timeStr, TIME_FMT);
            } catch (Exception e) {
                // 执行时间格式异常时不判断状态时间，仅按日期判断
            }
            Set<Long> planRelIds = parseRelIds(plan.getRelIds());

            LocalDate current = monthStart;
            while (!current.isAfter(monthEnd)) {
                if (!current.isBefore(startDate) && !current.isAfter(endDate)) {
                    String dayOfWeek = String.valueOf(current.getDayOfWeek().getValue());
                    if (enabledWeekDays.isEmpty() || enabledWeekDays.contains(dayOfWeek)) {
                        String dateKey = current.format(DATE_FMT);
                        String executeLogStatus = lookupExecuteLogStatus(executeLogStatusByDate, dateKey, plan.getId());
                        CalendarEvent event = new CalendarEvent();
                        event.setSource("PLAN");
                        event.setPlanId(plan.getId());
                        event.setPlanName(plan.getPlanName());
                        event.setLabel(timeStr + " " + operationLabel);
                        event.setColor(color);
                        event.setPlanType(plan.getPlanType());
                        event.setOperationType(plan.getOperationType());
                        event.setStatus(resolveStatus(current, execTime, executedRelIdsByDate, planRelIds, executeLogStatus));

                        dayEventMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(event);
                    }
                }
                current = current.plusDays(1);
            }
        }
    }

    // ==================== MQ 执行日志（执行成功/执行失败判定） ====================

    /**
     * 查询指定月份的 MQ 执行日志（lighting_plan_execute_log）
     * 返回 日期(yyyy-MM-dd) → (计划ID → 状态) 映射，用于判定 执行成功/执行失败
     */
    private Map<String, Map<Long, String>> loadMonthExecuteLogs(LocalDate monthStart, LocalDate monthEnd) {
        Map<String, Map<Long, String>> map = new HashMap<>();
        List<LightingPlanExecuteLog> list = lightingPlanExecuteLogService.list(
                new LambdaQueryWrapper<LightingPlanExecuteLog>()
                        .ge(LightingPlanExecuteLog::getExecuteDate, monthStart.format(DATE_FMT))
                        .le(LightingPlanExecuteLog::getExecuteDate, monthEnd.format(DATE_FMT))
                        .orderByDesc(LightingPlanExecuteLog::getId));
        if (list == null || list.isEmpty()) return map;
        for (LightingPlanExecuteLog logEntry : list) {
            if (logEntry.getExecuteDate() == null || logEntry.getPlanId() == null) {
                continue;
            }
            // 同一天同一计划可能有多条记录，保留最新一条（list 已按 id 倒序，首个写入后不再覆盖）
            map.computeIfAbsent(logEntry.getExecuteDate(), k -> new HashMap<>())
                    .putIfAbsent(logEntry.getPlanId(), logEntry.getStatus());
        }
        return map;
    }

    /**
     * 从月度执行日志映射中查询指定日期+计划的执行状态
     */
    private String lookupExecuteLogStatus(Map<String, Map<Long, String>> executeLogStatusByDate, String dateKey, Long planId) {
        if (executeLogStatusByDate == null || planId == null) return null;
        Map<Long, String> dayMap = executeLogStatusByDate.get(dateKey);
        return dayMap == null ? null : dayMap.get(planId);
    }

    /**
     * 按执行日期查询指定计划的 MQ 执行状态（详情接口用）
     */
    private String lookupExecuteLogStatus(String date, Long planId) {
        if (date == null || planId == null) return null;
        LightingPlanExecuteLog latest = lightingPlanExecuteLogService.getOne(
                new LambdaQueryWrapper<LightingPlanExecuteLog>()
                        .eq(LightingPlanExecuteLog::getExecuteDate, date)
                        .eq(LightingPlanExecuteLog::getPlanId, planId)
                        .orderByDesc(LightingPlanExecuteLog::getId)
                        .last("LIMIT 1"), false);
        return latest == null ? null : latest.getStatus();
    }

    /**
     * 详情接口状态判定：
     * - 未来（执行时间未到）→ 待执行
     * - 执行时间已过：优先 MQ 执行日志（成功→执行成功，失败/待消费→执行失败）
     * - 无执行日志时按操作日志兜底（有日志→执行成功，无→待执行）
     */
    private String resolveDetailStatus(String date, String executionTime, String executeLogStatus, List<LightingOperationLog> logs) {
        // 未来时间 → 待执行
        try {
            LocalDate day = LocalDate.parse(date, DATE_FMT);
            if (StringUtils.isNotEmpty(executionTime)) {
                LocalDateTime eventDateTime = day.atTime(LocalTime.parse(executionTime, TIME_FMT));
                if (eventDateTime.isAfter(LocalDateTime.now())) {
                    return STATUS_PENDING;
                }
            } else if (day.isAfter(LocalDate.now())) {
                return STATUS_PENDING;
            }
        } catch (Exception ignored) {
            // 日期/时间格式异常时不判断未来，继续走日志判定
        }
        if (StringUtils.isNotEmpty(executeLogStatus)) {
            if (LightingPlanExecuteLog.STATUS_SUCCESS.equals(executeLogStatus)) {
                return STATUS_SUCCESS;
            }
            // 执行失败 / 待消费（已发送未消费）→ 执行失败
            return STATUS_FAIL;
        }
        return (logs != null && !logs.isEmpty()) ? STATUS_SUCCESS : STATUS_PENDING;
    }

    // ==================== 控制日志（仅用于状态兜底判定，不作为日历事件） ====================

    /**
     * 查询指定月份的当月控制日志，供 buildExecutedRelIdsByDate 判断“已执行”状态（兜底）。
     * 执行日志不作为独立事件展示在日历上，详情请调用 /detail 接口。
     */
    private List<LightingOperationLog> loadMonthLogs(LocalDateTime monthStart, LocalDateTime monthEnd) {
        return lightingOperationLogService.list(
                new LambdaQueryWrapper<LightingOperationLog>()
                        .ge(LightingOperationLog::getOperationTime, monthStart)
                        .le(LightingOperationLog::getOperationTime, monthEnd)
                        .orderByDesc(LightingOperationLog::getOperationTime)
        );
    }

    /**
     * 构建 “日期 → 当天已执行的目标ID集合” 映射（用于判断计划/定时任务是否已执行，兜底）。
     * 仅统计计划/定时器触发的操作日志（操作人=定时器/照明计划），排除手动操作，
     * 避免“用户手动开灯”将同一天的定时计划误判为已执行。
     */
    private Map<String, Set<Long>> buildExecutedRelIdsByDate(List<LightingOperationLog> logs) {
        Map<String, Set<Long>> map = new HashMap<>();
        if (logs == null || logs.isEmpty()) return map;
        for (LightingOperationLog logEntry : logs) {
            if (logEntry.getOperationTime() == null || logEntry.getRelId() == null) {
                continue;
            }
            String operator = logEntry.getOperationBy();
            if (!OPERATOR_TIMER.equals(operator) && !OPERATOR_PLAN.equals(operator)) {
                continue;
            }
            String dateKey = logEntry.getOperationTime().toLocalDate().format(DATE_FMT);
            map.computeIfAbsent(dateKey, k -> new HashSet<>()).add(logEntry.getRelId());
        }
        return map;
    }

    /**
     * 解析逗号分隔的ID集合（非法ID忽略）
     */
    private Set<Long> parseRelIds(String relIds) {
        Set<Long> ids = new HashSet<>();
        if (StringUtils.isBlank(relIds)) return ids;
        for (String s : relIds.split(",")) {
            if (StringUtils.isBlank(s)) continue;
            try {
                ids.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略非法ID
            }
        }
        return ids;
    }


    /**
     * 判定事件状态：
     * - 执行时间在将来 → 待执行
     * - 执行时间已到/已过，且有 MQ 执行日志：
     *   - 执行成功 → 执行成功
     *   - 执行失败 / 待消费（已发送未消费）→ 执行失败
     * - 无 MQ 执行日志（历史数据），按当天计划执行日志兜底：
     *   - 已执行 → 执行成功；否则 → 待执行（未执行）
     */
    private String resolveStatus(LocalDate date, LocalTime execTime,
                                 Map<String, Set<Long>> executedRelIdsByDate, Set<Long> relIds,
                                 String executeLogStatus) {
        LocalDateTime now = LocalDateTime.now();
        if (execTime != null) {
            LocalDateTime eventDateTime = date.atTime(execTime);
            if (eventDateTime.isAfter(now)) {
                return STATUS_PENDING;
            }
        } else if (date.isAfter(now.toLocalDate())) {
            return STATUS_PENDING;
        }
        // 执行时间已到/已过：优先按 MQ 执行日志判定
        if (StringUtils.isNotEmpty(executeLogStatus)) {
            if (LightingPlanExecuteLog.STATUS_SUCCESS.equals(executeLogStatus)) {
                return STATUS_SUCCESS;
            }
            // 执行失败 / 待消费（已发送但未被消费）→ 执行失败
            return STATUS_FAIL;
        }
        // 无执行日志（历史数据）：通过当天计划执行日志判断是否真正执行
        if (isExecutedOn(executedRelIdsByDate, date, relIds)) {
            return STATUS_SUCCESS;
        }
        return STATUS_PENDING;
    }

    /**
     * 判断某天是否已对目标集合执行过控制（存在匹配的计划/定时器日志）
     */
    private boolean isExecutedOn(Map<String, Set<Long>> executedRelIdsByDate, LocalDate date, Set<Long> relIds) {
        if (relIds == null || relIds.isEmpty()) return false;
        Set<Long> executedIds = executedRelIdsByDate.get(date.format(DATE_FMT));
        if (executedIds == null || executedIds.isEmpty()) return false;
        for (Long id : relIds) {
            if (executedIds.contains(id)) return true;
        }
        return false;
    }


    // ==================== DTO ====================

    @Data
    @ApiModel("日历日事件")
    public static class CalendarDayEvent {
        @ApiModelProperty("日期 yyyy-MM-dd")
        private String date;
        @ApiModelProperty("星期几 1-7 (1=周一)")
        private String dayOfWeek;
        @ApiModelProperty("当日事件列表")
        private List<CalendarEvent> events;
    }

    @Data
    @ApiModel("日历事件")
    public static class CalendarEvent {
        @ApiModelProperty("来源：PLAN-照明计划")
        private String source;
        @ApiModelProperty("关联ID（雪花ID，序列化为字符串避免精度丢失）")
        @JsonSerialize(using = ToStringSerializer.class)
        private Long planId;
        @ApiModelProperty("名称")
        private String planName;
        @ApiModelProperty("事件标签（时间+操作）")
        private String label;
        @ApiModelProperty("颜色标识：blue=计划 green=节日")
        private String color;
        @ApiModelProperty("计划类型/来源描述")
        private String planType;
        @ApiModelProperty("操作类型：开灯/关灯/执行")
        private String operationType;
        @ApiModelProperty("状态：待执行/执行成功/执行失败")
        private String status;
    }

    @Data
    @ApiModel("日历事件详情")
    public static class CalendarEventDetail {
        @ApiModelProperty("来源：PLAN-照明计划 LOG-历史日志")
        private String source;
        @ApiModelProperty("关联ID（雪花ID，序列化为字符串避免精度丢失）")
        @JsonSerialize(using = ToStringSerializer.class)
        private Long planId;
        @ApiModelProperty("日期 yyyy-MM-dd")
        private String date;
        @ApiModelProperty("名称")
        private String planName;
        @ApiModelProperty("关联类型：区域、回路")
        private String relType;
        @ApiModelProperty("操作类型：开启/关闭/执行")
        private String operationType;
        @ApiModelProperty("计划执行时间 HH:mm:ss")
        private String executionTime;
        @ApiModelProperty("状态：待执行/执行成功/执行失败")
        private String status;
        @ApiModelProperty("执行日志列表（当天有执行记录时返回）")
        private List<LightingOperationLog> logs;
        @ApiModelProperty("执行目标列表（当天无执行日志时返回，要执行的区域/回路）")
        private List<CalendarTarget> targets;
    }

    @Data
    @ApiModel("日历事件目标")
    public static class CalendarTarget {
        @ApiModelProperty("目标ID（雪花ID，序列化为字符串避免精度丢失）")
        @JsonSerialize(using = ToStringSerializer.class)
        private Long relId;
        @ApiModelProperty("目标名称")
        private String relName;
        @ApiModelProperty("目标类型：区域、回路")
        private String relType;
        @ApiModelProperty("电流（A），来自 lighting_circuit.electric_current，仅回路有值")
        private Double electricCurrent;
    }
}
