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
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecutionTimeService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 控制日历
 */
@Api(tags = "照明-控制日历")
@Slf4j
@RestController
@RequestMapping("/bems/lighting/calendar")
@AllArgsConstructor
public class LightingCalendarController {

    private final ILightingPlanService planService;

    private final ILightingPlanExecutionTimeService executionTimeService;

    /**
     * 查询指定年月的日历事件
     * @param year 年份
     * @param month 月份 1-12
     */
    @ApiOperation("查询指定年月的日历事件")
    @GetMapping("/events")
    public Result<List<CalendarDayEvent>> events(@RequestParam int year, @RequestParam int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalMonthRange range = new LocalMonthRange(yearMonth.atDay(1), yearMonth.atEndOfMonth());

        List<LightingPlan> enabledPlans = planService.list(
                new LambdaQueryWrapper<LightingPlan>()
                        .eq(LightingPlan::getStatus, LightingPlan.STATUS_ENABLE)
        );

        if (enabledPlans.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        Map<Long, LightingPlanExecutionTime> executionTimeMap = executionTimeService.getByPlanIds(
                enabledPlans.stream().map(LightingPlan::getId).toList()
        ).stream().collect(Collectors.toMap(LightingPlanExecutionTime::getPlanId, et -> et, (a, b) -> a));

        Map<String, List<CalendarEvent>> dayEventMap = new HashMap<>();

        for (LightingPlan plan : enabledPlans) {
            LightingPlanExecutionTime et = executionTimeMap.get(plan.getId());
            if (et == null) {
                continue;
            }

            LocalDate startDate;
            LocalDate endDate;
            try {
                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                startDate = LocalDate.parse(et.getStartDate(), df);
                endDate = LocalDate.parse(et.getEndDate(), df);
            } catch (Exception e) {
                log.warn("解析计划时间配置失败, planId={}", plan.getId());
                continue;
            }

            if (startDate.isAfter(range.end) || endDate.isBefore(range.start)) {
                continue;
            }

            Set<String> enabledWeekDays = new HashSet<>();
            if (et.getEnabledWeek() != null) {
                enabledWeekDays.addAll(Arrays.asList(et.getEnabledWeek().split(",")));
            }

            String operationLabel = LightingPlan.OPERATION_TYPE_OPEN.equals(plan.getOperationType()) ? "开灯" : "关灯";
            String timeStr = et.getExecutionTime() != null ? et.getExecutionTime() : "";
            String eventLabel = timeStr + " " + operationLabel;
            String color = plan.getPlanType() != null && plan.getPlanType().contains("节日") ? "green" : "blue";

            LocalDate current = range.start;
            while (!current.isAfter(range.end)) {
                if (!current.isBefore(startDate) && !current.isAfter(endDate)) {
                    String dayOfWeek = String.valueOf(current.getDayOfWeek().getValue());
                    if (enabledWeekDays.isEmpty() || enabledWeekDays.contains(dayOfWeek)) {
                        String dateKey = current.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        CalendarEvent event = new CalendarEvent();
                        event.setPlanId(plan.getId());
                        event.setPlanName(plan.getPlanName());
                        event.setLabel(eventLabel);
                        event.setColor(color);
                        event.setPlanType(plan.getPlanType());
                        event.setOperationType(plan.getOperationType());

                        dayEventMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(event);
                    }
                }
                current = current.plusDays(1);
            }
        }

        List<CalendarDayEvent> result = new ArrayList<>();
        for (LocalDate date = range.start; !date.isAfter(range.end); date = date.plusDays(1)) {
            String dateKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            CalendarDayEvent dayEvent = new CalendarDayEvent();
            dayEvent.setDate(dateKey);
            dayEvent.setDayOfWeek(String.valueOf(date.getDayOfWeek().getValue()));
            dayEvent.setEvents(dayEventMap.getOrDefault(dateKey, Collections.emptyList()));
            result.add(dayEvent);
        }

        return Result.ok(result);
    }

    private record LocalMonthRange(LocalDate start, LocalDate end) {}

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
        @ApiModelProperty("计划ID")
        private Long planId;
        @ApiModelProperty("计划名称")
        private String planName;
        @ApiModelProperty("事件标签（时间+操作）")
        private String label;
        @ApiModelProperty("颜色标识：blue=普通, green=节日")
        private String color;
        @ApiModelProperty("计划类型")
        private String planType;
        @ApiModelProperty("操作类型：开灯/关灯")
        private String operationType;
    }
}