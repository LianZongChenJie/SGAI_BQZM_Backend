package org.jeecg.modules.bems.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态定时任务管理器
 *
 * 支持两种任务模式：
 * 1. 灯光控制模式（ScheduleJob.controlType = AREA/CIRCUIT）
 *    直接通过 LightingService 发送控制指令，并通过 ILightingOperationLogService 记录操作日志（操作人="定时器"）
 * 2. 通用反射模式（ScheduleJob.controlType = null）
 *    通过反射调用指定的 Spring Bean 方法
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
                log.info("已加载定时任务: {} [{}] cron: {}", job.getJobName(), job.getId(), job.getCronExpression());
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

        ScheduledFuture<?> future = taskScheduler.schedule(task, triggerContext -> {
            CronTrigger trigger = new CronTrigger(job.getCronExpression());
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
        log.info("定时任务已注册: {} [{}] cron: {} -> {}", job.getJobName(), job.getId(), job.getCronExpression(), describeJob(job));
    }

    /**
     * 根据任务配置创建可执行逻辑
     */
    private Runnable createTask(ScheduleJob job) {
        if ("AREA".equals(job.getControlType())) {
            return createAreaControlTask(job);
        }
        if ("CIRCUIT".equals(job.getControlType())) {
            return createCircuitControlTask(job);
        }
        return createReflectionTask(job);
    }

    /**
     * 区域灯光控制任务
     * 流程：查找区域 → 发送MQ场景控制指令 → 记录操作日志（操作人=定时器）
     */
    private Runnable createAreaControlTask(ScheduleJob job) {
        if (job.getTargetId() == null) {
            throw new RuntimeException("区域控制任务 targetId 不能为空, jobName=" + job.getJobName());
        }
        final Long areaId = job.getTargetId();
        final boolean isOpen = "OPEN".equals(job.getOperationType());

        // 预检：区域是否存在
        LightingArea area = lightingAreaService.getById(areaId);
        if (area == null) {
            throw new RuntimeException("区域不存在, areaId=" + areaId);
        }
        final String space = area.getSpace();
        final String areaCode = area.getAreaCode();
        final String openCode = area.getOpenCode();
        final String closeCode = area.getCloseCode();
        final String areaName = area.getAreaName();

        return () -> {
            try {
                long startTime = System.currentTimeMillis();
                String actionName = isOpen ? "区域全开" : "区域全关";
                log.info("定时任务 - {}: areaId={}", actionName, areaId);

                // 1. 发送 MQ 控制指令
                if (isOpen) {
                    lightingService.areaOpen(space, areaCode, openCode);
                } else {
                    lightingService.areaClose(space, areaCode, closeCode);
                }

                // 2. 记录操作日志（操作人=定时器）
                lightingOperationLogService.saveLog(
                        LightingOperationLog.REL_TYPE_AREA,
                        areaId,
                        areaName,
                        LocalDateTime.now(),
                        actionName,
                        "定时器"
                );

                long cost = System.currentTimeMillis() - startTime;
                log.info("定时任务 - {}完成: areaId={}, 耗时={}ms", actionName, areaId, cost);
                updateLastRunTime(job);
            } catch (Exception e) {
                log.error("定时任务 - 区域控制异常: areaId={}", areaId, e);
            }
        };
    }

    /**
     * 回路灯光控制任务
     * 流程：查找回路 → 发送MQ回路控制指令 → 记录操作日志（操作人=定时器）
     */
    private Runnable createCircuitControlTask(ScheduleJob job) {
        if (job.getTargetId() == null) {
            throw new RuntimeException("回路控制任务 targetId 不能为空, jobName=" + job.getJobName());
        }
        final Long circuitId = job.getTargetId();
        final boolean isOpen = "OPEN".equals(job.getOperationType());

        // 预检：回路是否存在
        LightingCircuit circuit = lightingCircuitService.getById(circuitId);
        if (circuit == null) {
            throw new RuntimeException("回路不存在, circuitId=" + circuitId);
        }
        // 获取回路所属区域信息
        LightingArea area = lightingAreaService.getById(circuit.getAreaId());
        if (area == null) {
            throw new RuntimeException("回路所属区域不存在, circuitId=" + circuitId);
        }
        final String space = area.getSpace();
        final String areaCode = area.getAreaCode();
        final String circuitCode = circuit.getCircuitCode();
        final String circuitDisplayName = area.getAreaName() + "-" + circuit.getCircuitName();

        return () -> {
            try {
                long startTime = System.currentTimeMillis();
                String actionName = isOpen ? "回路开启" : "回路关闭";
                log.info("定时任务 - {}: circuitId={}", actionName, circuitId);

                // 1. 发送 MQ 控制指令
                if (isOpen) {
                    lightingService.circuitOpen(space, areaCode, circuitCode);
                } else {
                    lightingService.circuitClose(space, areaCode, circuitCode);
                }

                // 2. 记录操作日志（操作人=定时器）
                lightingOperationLogService.saveLog(
                        LightingOperationLog.REL_TYPE_CIRCUIT,
                        circuitId,
                        circuitDisplayName,
                        LocalDateTime.now(),
                        actionName,
                        "定时器"
                );

                long cost = System.currentTimeMillis() - startTime;
                log.info("定时任务 - {}完成: circuitId={}, 耗时={}ms", actionName, circuitId, cost);
                updateLastRunTime(job);
            } catch (Exception e) {
                log.error("定时任务 - 回路控制异常: circuitId={}", circuitId, e);
            }
        };
    }

    /**
     * 通用反射模式任务
     */
    private Runnable createReflectionTask(ScheduleJob job) {
        try {
            Object bean = applicationContext.getBean(job.getBeanName());
            Method method = bean.getClass().getMethod(job.getMethodName());

            return () -> {
                try {
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
     * 立即执行一次定时任务
     */
    public void executeJob(ScheduleJob job) {
        Runnable task = createTask(job);
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
        if ("AREA".equals(job.getControlType())) {
            String action = "OPEN".equals(job.getOperationType()) ? "开灯" : "关灯";
            return "区域[" + job.getTargetId() + "] " + action;
        }
        if ("CIRCUIT".equals(job.getControlType())) {
            String action = "OPEN".equals(job.getOperationType()) ? "开灯" : "关灯";
            return "回路[" + job.getTargetId() + "] " + action;
        }
        return job.getBeanName() + "." + job.getMethodName() + "()";
    }
}
