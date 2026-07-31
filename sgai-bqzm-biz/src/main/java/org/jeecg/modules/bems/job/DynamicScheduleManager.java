package org.jeecg.modules.bems.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.entity.ScheduleJob;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.LightingService;
import org.jeecg.modules.bems.mapper.ScheduleJobMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * 动态定时任务管理器
 *
 * 支持两种任务模式：
 * 1. 灯光控制模式（controlType=AREA/CIRCUIT 或 relType=区域/回路，支持多目标）
 *    直接通过 LightingService 发送控制指令，并通过 ILightingOperationLogService 记录操作日志（操作人="定时器"）
 * 2. 通用反射模式（controlType=null）
 *    通过反射调用指定的 Spring Bean 方法
 *
 * 时间配置（用户友好，与照明计划接口一致）：
 * - executionTime(HH:mm:ss) + cycleType(每天/工作日/周末/自定义) + enabledWeek(1-7) 自动生成 cron
 * - startDate/endDate 生效日期范围（定时触发时判断，手动执行不判断）
 * - 高级用户可直接填 cronExpression，优先级最高
 *
 * 注意：Spring 的 CronTrigger 使用 6 字段 cron（秒 分 时 日 月 周），
 * 例如 "00 00 18 * * ?" 表示每天 18:00:00 执行。
 *
 * 流程：
 * 1. 系统启动时从 DB 加载已启用的任务并注册到调度器
 * 2. 提供动态添加/删除/更新定时任务的方法
 * 3. 定时到点自动执行：发送MQ控制指令 → 记录操作日志
 */
@Slf4j
@Component
public class DynamicScheduleManager {

    private final ApplicationContext applicationContext;
    private final ScheduleJobMapper scheduleJobMapper;
    private final ILightingAreaService lightingAreaService;
    private final ILightingCircuitService lightingCircuitService;
    private final LightingService lightingService;
    private final ILightingOperationLogService lightingOperationLogService;
    private ThreadPoolTaskScheduler taskScheduler;

    /** 存储已注册的任务 future，key 为 job id */
    private final Map<Long, ScheduledFuture<?>> scheduledFutureMap = new ConcurrentHashMap<>();

    public DynamicScheduleManager(ApplicationContext applicationContext,
                                   ScheduleJobMapper scheduleJobMapper,
                                   ILightingAreaService lightingAreaService,
                                   ILightingCircuitService lightingCircuitService,
                                   LightingService lightingService,
                                   ILightingOperationLogService lightingOperationLogService) {
        this.applicationContext = applicationContext;
        this.scheduleJobMapper = scheduleJobMapper;
        this.lightingAreaService = lightingAreaService;
        this.lightingCircuitService = lightingCircuitService;
        this.lightingService = lightingService;
        this.lightingOperationLogService = lightingOperationLogService;
    }

    @PostConstruct
    public void init() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(20);
        taskScheduler.setThreadNamePrefix("dynamic-schedule-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(10);
        taskScheduler.initialize();

        loadAllEnabledJobs();
    }

    /**
     * 加载所有已启用的定时任务
     */
    private void loadAllEnabledJobs() {
        List<ScheduleJob> enabledJobs = scheduleJobMapper.selectList(
                new LambdaQueryWrapper<ScheduleJob>().eq(ScheduleJob::getStatus, 1)
        );
        for (ScheduleJob job : enabledJobs) {
            try {
                addJob(job);
                log.info("已加载定时任务: {} [{}] -> {}", job.getJobName(), job.getId(), describeJob(job));
            } catch (Exception e) {
                log.error("加载定时任务失败: {}，原因: {}", job.getJobName(), e.getMessage());
            }
        }
        log.info("动态定时任务加载完成，共加载 {} 个任务", enabledJobs.size());
    }

    /**
     * 添加一个定时任务到调度器
     */
    public void addJob(ScheduleJob job) {
        removeJob(job.getId());
        Runnable task = createTask(job);
        String cron = buildCron(job);

        // 自动生成的 cron 回写并持久化，保证日历接口等依赖 cronExpression 的地方可用
        if (StringUtils.isBlank(job.getCronExpression())) {
            ScheduleJob updateJob = new ScheduleJob();
            updateJob.setId(job.getId());
            updateJob.setCronExpression(cron);
            scheduleJobMapper.updateById(updateJob);
            job.setCronExpression(cron);
        }

        ScheduledFuture<?> future = taskScheduler.schedule(task, triggerContext -> {
            CronTrigger trigger = new CronTrigger(cron);
            java.util.Date nextExecTime = trigger.nextExecutionTime(triggerContext);
            if (nextExecTime != null) {
                ScheduleJob updateJob = new ScheduleJob();
                updateJob.setId(job.getId());
                updateJob.setNextRunTime(nextExecTime);
                scheduleJobMapper.updateById(updateJob);
            }
            return nextExecTime;
        });

        scheduledFutureMap.put(job.getId(), future);
        log.info("定时任务已注册: {} [{}] cron: {} -> {}", job.getJobName(), job.getId(), cron, describeJob(job));
    }

    /**
     * 构建 cron 表达式（public，供 Controller 保存/编辑时生成 cron 落库）
     *
     * 优先使用 cronExpression（高级用户），否则根据 executionTime + cycleType + enabledWeek 自动生成。
     * Spring cron 为 6 字段：秒 分 时 日 月 周，例如 "00 00 18 * * ?" = 每天 18:00:00。
     */
    public String buildCron(ScheduleJob job) {
        // 高级模式：直接填 cron 表达式
        if (StringUtils.isNotBlank(job.getCronExpression())) {
            return job.getCronExpression();
        }
        // 用户友好模式：执行时间 + 周期
        if (StringUtils.isBlank(job.getExecutionTime())) {
            throw new RuntimeException("定时任务缺少执行时间（executionTime 或 cronExpression），jobName=" + job.getJobName());
        }
        String[] parts = job.getExecutionTime().split(":");
        if (parts.length != 3) {
            throw new RuntimeException("执行时间格式错误，应为 HH:mm:ss，实际: " + job.getExecutionTime());
        }
        String second = parts[2];
        String minute = parts[1];
        String hour = parts[0];
        String cycleType = StringUtils.isBlank(job.getCycleType()) ? "每天" : job.getCycleType();
        switch (cycleType) {
            case "每天":
                return String.format("%s %s %s * * ?", second, minute, hour);
            case "工作日":
                return String.format("%s %s %s ? * MON-FRI", second, minute, hour);
            case "周末":
                return String.format("%s %s %s ? * SAT,SUN", second, minute, hour);
            case "自定义":
                if (StringUtils.isBlank(job.getEnabledWeek())) {
                    throw new RuntimeException("周期类型为自定义时必须填写 enabledWeek（1-7，1=周一，多个逗号分隔）");
                }
                String days = Arrays.stream(job.getEnabledWeek().split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .map(DynamicScheduleManager::weekNumberToName)
                        .collect(Collectors.joining(","));
                return String.format("%s %s %s ? * %s", second, minute, hour, days);
            default:
                throw new RuntimeException("不支持的周期类型: " + cycleType);
        }
    }

    /**
     * 周几数字转英文缩写（1=周一 ... 7=周日）
     */
    private static String weekNumberToName(String dayNum) {
        switch (dayNum) {
            case "1": return "MON";
            case "2": return "TUE";
            case "3": return "WED";
            case "4": return "THU";
            case "5": return "FRI";
            case "6": return "SAT";
            case "7": return "SUN";
            default: throw new RuntimeException("enabledWeek 取值必须为 1-7（1=周一），实际: " + dayNum);
        }
    }

    /**
     * 判断今天是否在生效日期范围内（startDate ~ endDate）
     * 未配置生效日期时默认为每天都生效
     */
    private boolean isInDateRange(ScheduleJob job) {
        LocalDate today = LocalDate.now();
        try {
            if (StringUtils.isNotBlank(job.getStartDate())) {
                LocalDate start = LocalDate.parse(job.getStartDate());
                if (today.isBefore(start)) {
                    return false;
                }
            }
            if (StringUtils.isNotBlank(job.getEndDate())) {
                LocalDate end = LocalDate.parse(job.getEndDate());
                if (today.isAfter(end)) {
                    return false;
                }
            }
        } catch (Exception e) {
            log.warn("定时任务生效日期格式错误，忽略日期范围限制。jobName={}, startDate={}, endDate={}",
                    job.getJobName(), job.getStartDate(), job.getEndDate());
        }
        return true;
    }

    /**
     * 根据任务配置创建可执行逻辑（定时触发，检查生效日期范围）
     */
    private Runnable createTask(ScheduleJob job) {
        return createTask(job, true);
    }

    /**
     * 创建任务
     * @param job 任务配置
     * @param checkDateRange 是否检查生效日期范围（定时触发为 true，手动执行为 false）
     */
    private Runnable createTask(ScheduleJob job, boolean checkDateRange) {
        if ("AREA".equals(job.getControlType()) || "区域".equals(job.getRelType())) {
            return createAreaControlTask(job, checkDateRange);
        }
        if ("CIRCUIT".equals(job.getControlType()) || "回路".equals(job.getRelType())) {
            return createCircuitControlTask(job, checkDateRange);
        }
        return createReflectionTask(job, checkDateRange);
    }

    /**
     * 解析控制目标ID集合
     * 优先取 relIds（逗号分隔的多目标），为空时回退到 targetId（单目标）
     */
    private List<Long> resolveTargetIds(ScheduleJob job) {
        List<Long> ids = new ArrayList<>();
        if (StringUtils.isNotBlank(job.getRelIds())) {
            for (String s : job.getRelIds().split(",")) {
                if (StringUtils.isNotBlank(s)) {
                    try {
                        ids.add(Long.parseLong(s.trim()));
                    } catch (NumberFormatException ignored) {
                        log.warn("定时任务目标ID格式错误，忽略: {}", s);
                    }
                }
            }
        }
        if (ids.isEmpty() && job.getTargetId() != null) {
            ids.add(job.getTargetId());
        }
        return ids;
    }

    /**
     * 区域灯光控制任务（支持多区域）
     * 流程：查找区域 → 发送MQ场景控制指令 → 记录操作日志（操作人=定时器）
     */
    private Runnable createAreaControlTask(ScheduleJob job, boolean checkDateRange) {
        List<Long> areaIds = resolveTargetIds(job);
        if (areaIds.isEmpty()) {
            throw new RuntimeException("区域控制任务 targetId/relIds 不能为空, jobName=" + job.getJobName());
        }
        final boolean isOpen = "OPEN".equals(job.getOperationType());

        // 预检：区域是否存在
        List<LightingArea> areas = lightingAreaService.listByIds(areaIds);
        if (areas.size() != new HashSet<>(areaIds).size()) {
            throw new RuntimeException("部分区域不存在, areaIds=" + areaIds);
        }

        return () -> {
            try {
                // 判断生效日期范围（手动执行时跳过该判断）
                if (checkDateRange && !isInDateRange(job)) {
                    log.info("定时任务 - {} 不在生效日期范围内，跳过本次执行", job.getJobName());
                    return;
                }
                long startTime = System.currentTimeMillis();
                String actionName = isOpen ? "区域全开" : "区域全关";
                log.info("定时任务 - {}: areaIds={}", actionName, areaIds);

                for (LightingArea area : areas) {
                    // 1. 发送 MQ 控制指令
                    if (isOpen) {
                        lightingService.areaOpen(area.getSpace(), area.getAreaCode(), area.getOpenCode());
                    } else {
                        lightingService.areaClose(area.getSpace(), area.getAreaCode(), area.getCloseCode());
                    }
                    // 2. 记录操作日志（操作人=定时器）
                    lightingOperationLogService.saveLog(
                            LightingOperationLog.REL_TYPE_AREA,
                            area.getId(),
                            area.getAreaName(),
                            LocalDateTime.now(),
                            actionName,
                            "定时器"
                    );
                }

                long cost = System.currentTimeMillis() - startTime;
                log.info("定时任务 - {}完成: areaIds={}, 耗时={}ms", actionName, areaIds, cost);
                updateLastRunTime(job);
            } catch (Exception e) {
                log.error("定时任务 - 区域控制异常: areaIds={}", areaIds, e);
            }
        };
    }

    /**
     * 回路灯光控制任务（支持多回路）
     * 流程：查找回路 → 发送MQ回路控制指令 → 记录操作日志（操作人=定时器）
     */
    private Runnable createCircuitControlTask(ScheduleJob job, boolean checkDateRange) {
        List<Long> circuitIds = resolveTargetIds(job);
        if (circuitIds.isEmpty()) {
            throw new RuntimeException("回路控制任务 targetId/relIds 不能为空, jobName=" + job.getJobName());
        }
        final boolean isOpen = "OPEN".equals(job.getOperationType());

        // 预检：回路是否存在
        List<LightingCircuit> circuits = lightingCircuitService.listByIds(circuitIds);
        if (circuits.size() != new HashSet<>(circuitIds).size()) {
            throw new RuntimeException("部分回路不存在, circuitIds=" + circuitIds);
        }

        return () -> {
            try {
                // 判断生效日期范围（手动执行时跳过该判断）
                if (checkDateRange && !isInDateRange(job)) {
                    log.info("定时任务 - {} 不在生效日期范围内，跳过本次执行", job.getJobName());
                    return;
                }
                long startTime = System.currentTimeMillis();
                String actionName = isOpen ? "回路开启" : "回路关闭";
                log.info("定时任务 - {}: circuitIds={}", actionName, circuitIds);

                for (LightingCircuit circuit : circuits) {
                    // 获取回路所属区域信息
                    LightingArea area = lightingAreaService.getById(circuit.getAreaId());
                    if (area == null) {
                        log.error("定时任务 - 回路所属区域不存在, circuitId={}", circuit.getId());
                        continue;
                    }
                    String circuitDisplayName = area.getAreaName() + "-" + circuit.getCircuitName();

                    // 1. 发送 MQ 控制指令
                    if (isOpen) {
                        lightingService.circuitOpen(area.getSpace(), area.getAreaCode(), circuit.getCircuitCode());
                    } else {
                        lightingService.circuitClose(area.getSpace(), area.getAreaCode(), circuit.getCircuitCode());
                    }
                    // 2. 记录操作日志（操作人=定时器）
                    lightingOperationLogService.saveLog(
                            LightingOperationLog.REL_TYPE_CIRCUIT,
                            circuit.getId(),
                            circuitDisplayName,
                            LocalDateTime.now(),
                            actionName,
                            "定时器"
                    );
                }

                long cost = System.currentTimeMillis() - startTime;
                log.info("定时任务 - {}完成: circuitIds={}, 耗时={}ms", actionName, circuitIds, cost);
                updateLastRunTime(job);
            } catch (Exception e) {
                log.error("定时任务 - 回路控制异常: circuitIds={}", circuitIds, e);
            }
        };
    }

    /**
     * 通用反射模式任务
     */
    private Runnable createReflectionTask(ScheduleJob job, boolean checkDateRange) {
        try {
            Object bean = applicationContext.getBean(job.getBeanName());
            Method method = bean.getClass().getMethod(job.getMethodName());

            return () -> {
                try {
                    // 判断生效日期范围（手动执行时跳过该判断）
                    if (checkDateRange && !isInDateRange(job)) {
                        log.info("定时任务 - {} 不在生效日期范围内，跳过本次执行", job.getJobName());
                        return;
                    }
                    long startTime = System.currentTimeMillis();
                    log.info("定时任务开始执行: {} [{}.{}]", job.getJobName(), job.getBeanName(), job.getMethodName());
                    method.invoke(bean);
                    long cost = System.currentTimeMillis() - startTime;
                    log.info("定时任务执行完成: {}，耗时: {}ms", job.getJobName(), cost);
                    updateLastRunTime(job);
                } catch (Exception e) {
                    log.error("定时任务执行异常: {}", job.getJobName(), e);
                }
            };

        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Bean '" + job.getBeanName() + "' 中未找到方法 '" + job.getMethodName() + "()'", e);
        } catch (Exception e) {
            throw new RuntimeException("注册定时任务失败: " + job.getJobName(), e);
        }
    }

    /**
     * 移除一个定时任务
     */
    public void removeJob(Long jobId) {
        ScheduledFuture<?> future = scheduledFutureMap.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("定时任务已移除: id={}", jobId);
        }
    }

    /**
     * 立即执行一次定时任务（手动执行：不检查生效日期范围）
     */
    public void executeJob(ScheduleJob job) {
        Runnable task = createTask(job, false);
        task.run();
    }

    /**
     * 更新上次执行时间
     */
    private void updateLastRunTime(ScheduleJob job) {
        ScheduleJob updateJob = new ScheduleJob();
        updateJob.setId(job.getId());
        updateJob.setLastRunTime(new java.util.Date());
        scheduleJobMapper.updateById(updateJob);
    }

    /**
     * 描述任务行为（用于日志）
     */
    private String describeJob(ScheduleJob job) {
        if ("AREA".equals(job.getControlType()) || "区域".equals(job.getRelType())) {
            String action = "OPEN".equals(job.getOperationType()) ? "开灯" : "关灯";
            return "区域" + resolveTargetIds(job) + " " + action;
        }
        if ("CIRCUIT".equals(job.getControlType()) || "回路".equals(job.getRelType())) {
            String action = "OPEN".equals(job.getOperationType()) ? "开灯" : "关灯";
            return "回路" + resolveTargetIds(job) + " " + action;
        }
        return job.getBeanName() + "." + job.getMethodName() + "()";
    }
}
