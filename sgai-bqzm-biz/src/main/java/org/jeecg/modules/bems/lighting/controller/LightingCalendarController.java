package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.entity.ScheduleJob;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecutionTimeService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.jeecg.modules.bems.service.IScheduleJobService;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 控制日历
 *
 * 显示指定月份内：
 * 1. 照明计划（LightingPlan）的执行日期
 * 2. 历史控制日志（LightingOperationLog）
 * 3. 动态定时任务（ScheduleJob）的预计执行日期
 */
@Api(tags = "照明-控制日历")
@Slf4j
@RestController
@RequestMapping("/bems/lighting/calendar")
@AllArgsConstructor
public class LightingCalendarController {

    private final ILightingPlanService planService;
    private final ILightingPlanExecutionTimeService executionTimeService;
    private final ILightingOperationLogService lightingOperationLogService;
    private final IScheduleJobService scheduleJobService;

    /**
     * 查询指定年月的日历事件
     * @param year 年份
     * @param month 月份 1-12
     */
    @ApiOperation("查询指定年月的日历事件（含照明计划、历史日志、动态定时任务）")
    @GetMapping("/events")
    public Result<List<CalendarDayEvent>> events(@RequestParam int year, @RequestParam int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        LocalDateTime monthStartTime = monthStart.atStartOfDay();
        LocalDateTime monthEndTime = monthEnd.atTime(23, 59, 59);

        Map<String, List<CalendarEvent>> dayEventMap = new HashMap<>();

        // 1. 照明计划事件
        loadPlanEvents(monthStart, monthEnd, dayEventMap);

        // 2. 历史控制日志事件
        loadOperationLogEvents(monthStartTime, monthEndTime, dayEventMap);

        // 3. 动态定时任务事件
        loadScheduleJobEvents(monthStart, monthEnd, dayEventMap);

        // 组装返回结果
        List<CalendarDayEvent> result = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
            String dateKey = date.format(df);
            CalendarDayEvent dayEvent = new CalendarDayEvent();
            dayEvent.setDate(dateKey);
            dayEvent.setDayOfWeek(String.valueOf(date.getDayOfWeek().getValue()));
            dayEvent.setEvents(dayEventMap.getOrDefault(dateKey, Collections.emptyList()));
            result.add(dayEvent);
        }

        return Result.ok(result);
    }

    // ==================== 照明计划 ====================

    private void loadPlanEvents(LocalDate monthStart, LocalDate monthEnd,
                                 Map<String, List<CalendarEvent>> dayEventMap) {
        List<LightingPlan> enabledPlans = planService.list(
                new LambdaQueryWrapper<LightingPlan>()
                        .eq(LightingPlan::getStatus, LightingPlan.STATUS_ENABLE)
        );

        if (enabledPlans.isEmpty()) return;

        Map<Long, LightingPlanExecutionTime> executionTimeMap = executionTimeService.getByPlanIds(
                enabledPlans.stream().map(LightingPlan::getId).toList()
        ).stream().collect(Collectors.toMap(LightingPlanExecutionTime::getPlanId, et -> et, (a, b) -> a));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (LightingPlan plan : enabledPlans) {
            LightingPlanExecutionTime et = executionTimeMap.get(plan.getId());
            if (et == null) continue;

            LocalDate startDate, endDate;
            try {
                startDate = LocalDate.parse(et.getStartDate(), dateFormatter);
                endDate = LocalDate.parse(et.getEndDate(), dateFormatter);
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

            LocalDate current = monthStart;
            while (!current.isAfter(monthEnd)) {
                if (!current.isBefore(startDate) && !current.isAfter(endDate)) {
                    String dayOfWeek = String.valueOf(current.getDayOfWeek().getValue());
                    if (enabledWeekDays.isEmpty() || enabledWeekDays.contains(dayOfWeek)) {
                        String dateKey = current.format(dateFormatter);
                        CalendarEvent event = new CalendarEvent();
                        event.setSource("PLAN");
                        event.setPlanId(plan.getId());
                        event.setPlanName(plan.getPlanName());
                        event.setLabel(timeStr + " " + operationLabel);
                        event.setColor(color);
                        event.setPlanType(plan.getPlanType());
                        event.setOperationType(plan.getOperationType());

                        dayEventMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(event);
                    }
                }
                current = current.plusDays(1);
            }
        }
    }

    // ==================== 历史控制日志 ====================

    private void loadOperationLogEvents(LocalDateTime monthStart, LocalDateTime monthEnd,
                                        Map<String, List<CalendarEvent>> dayEventMap) {
        List<LightingOperationLog> logs = lightingOperationLogService.list(
                new LambdaQueryWrapper<LightingOperationLog>()
                        .ge(LightingOperationLog::getOperationTime, monthStart)
                        .le(LightingOperationLog::getOperationTime, monthEnd)
                        .orderByDesc(LightingOperationLog::getOperationTime)
        );

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");

        for (LightingOperationLog logEntry : logs) {
            String dateKey = logEntry.getOperationTime().toLocalDate().format(df);
            String timeStr = logEntry.getOperationTime().format(tf);

            CalendarEvent event = new CalendarEvent();
            event.setSource("LOG");
            event.setPlanId(logEntry.getRelId());
            event.setPlanName(logEntry.getName());
            event.setLabel(timeStr + " " + logEntry.getOperationType() + " [" + logEntry.getOperationBy() + "]");
            event.setColor("gray");
            event.setPlanType("历史记录");
            event.setOperationType(logEntry.getOperationType());

            dayEventMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(event);
        }
    }

    // ==================== 动态定时任务 ====================

    private void loadScheduleJobEvents(LocalDate monthStart, LocalDate monthEnd,
                                       Map<String, List<CalendarEvent>> dayEventMap) {
        List<ScheduleJob> enabledJobs = scheduleJobService.list(
                new LambdaQueryWrapper<ScheduleJob>()
                        .eq(ScheduleJob::getStatus, 1)
        );

        if (enabledJobs.isEmpty()) return;

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (ScheduleJob job : enabledJobs) {
            // 1. nextRunTime 事件（未来）
            if (job.getNextRunTime() != null) {
                LocalDate nextDate = job.getNextRunTime().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                if (!nextDate.isBefore(monthStart) && !nextDate.isAfter(monthEnd)) {
                    String dateKey = nextDate.format(df);
                    String timeStr = job.getNextRunTime().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                    addScheduleEvent(dayEventMap, dateKey, timeStr, job);
                }
            }

            // 2. lastRunTime 事件（历史）
            if (job.getLastRunTime() != null) {
                LocalDate lastDate = job.getLastRunTime().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                if (!lastDate.isBefore(monthStart) && !lastDate.isAfter(monthEnd)) {
                    String dateKey = lastDate.format(df);
                    boolean alreadyAdded = dayEventMap.getOrDefault(dateKey, Collections.emptyList())
                            .stream().anyMatch(e ->
                                    e.getSource().equals("SCHEDULE") &&
                                    Objects.equals(e.getPlanId(), job.getId()));

                    if (!alreadyAdded) {
                        String timeStr = job.getLastRunTime().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalTime()
                                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                        CalendarEvent event = buildScheduleEvent(job, timeStr);
                        event.setLabel(timeStr + " " + getScheduleActionLabel(job) + " [已执行]");
                        dayEventMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(event);
                    }
                }
            }

            // 3. 通过 CronExpression.next() 推算当月内所有未来的执行日期
            try {
                if (job.getCronExpression() != null) {
                    CronExpression cronExpr = CronExpression.parse(job.getCronExpression());
                    LocalDateTime cursor = monthStart.atStartOfDay();
                    while (cursor != null && !cursor.toLocalDate().isAfter(monthEnd)) {
                        cursor = cronExpr.next(cursor);
                        if (cursor != null && !cursor.toLocalDate().isAfter(monthEnd)) {
                            String dateKey = cursor.toLocalDate().format(df);
                            String timeStr = cursor.toLocalTime()
                                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                            addScheduleEvent(dayEventMap, dateKey, timeStr, job);

                            cursor = cursor.plusNanos(1); // 推进 nanosecond，避免死循环
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析定时任务cron表达式失败, jobId={}, cron={}", job.getId(), job.getCronExpression());
            }
        }
    }

    /** 添加 Schedule 事件（去重） */
    private void addScheduleEvent(Map<String, List<CalendarEvent>> dayEventMap,
                                   String dateKey, String timeStr, ScheduleJob job) {
        boolean alreadyExists = dayEventMap.getOrDefault(dateKey, Collections.emptyList())
                .stream().anyMatch(e ->
                        e.getSource().equals("SCHEDULE") &&
                        e.getLabel() != null &&
                        e.getLabel().contains(timeStr) &&
                        Objects.equals(e.getPlanId(), job.getId()));

        if (!alreadyExists) {
            CalendarEvent event = buildScheduleEvent(job, timeStr);
            dayEventMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(event);
        }
    }

    /** 构建 Schedule 日历事件 */
    private CalendarEvent buildScheduleEvent(ScheduleJob job, String timeStr) {
        CalendarEvent event = new CalendarEvent();
        event.setSource("SCHEDULE");
        event.setPlanId(job.getId());
        event.setPlanName(job.getJobName());
        event.setLabel(timeStr + " " + getScheduleActionLabel(job));
        event.setColor("orange");
        event.setPlanType("动态任务");
        event.setOperationType(getScheduleOperationType(job));
        return event;
    }

    private String getScheduleActionLabel(ScheduleJob job) {
        if ("AREA".equals(job.getControlType())) {
            return "OPEN".equals(job.getOperationType()) ? "区域开灯" : "区域关灯";
        }
        if ("CIRCUIT".equals(job.getControlType())) {
            return "OPEN".equals(job.getOperationType()) ? "回路开灯" : "回路关灯";
        }
        return job.getBeanName() + "." + job.getMethodName();
    }

    private String getScheduleOperationType(ScheduleJob job) {
        if ("AREA".equals(job.getControlType()) || "CIRCUIT".equals(job.getControlType())) {
            return "OPEN".equals(job.getOperationType()) ? "开灯" : "关灯";
        }
        return "执行";
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
        @ApiModelProperty("来源：PLAN-照明计划 LOG-历史日志 SCHEDULE-动态任务")
        private String source;
        @ApiModelProperty("关联ID")
        private Long planId;
        @ApiModelProperty("名称")
        private String planName;
        @ApiModelProperty("事件标签（时间+操作）")
        private String label;
        @ApiModelProperty("颜色标识：blue=计划 green=节日 gray=历史 orange=动态任务")
        private String color;
        @ApiModelProperty("计划类型/来源描述")
        private String planType;
        @ApiModelProperty("操作类型：开灯/关灯/执行")
        private String operationType;
    }
}
