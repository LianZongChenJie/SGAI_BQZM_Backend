package org.jeecg.modules.bems.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.entity.ScheduleJob;
import org.jeecg.modules.bems.job.DynamicScheduleManager;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.jeecg.modules.bems.service.IScheduleJobService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 动态定时任务管理 API
 *
 * 前端页面通过此接口添加/修改/启停定时任务（包括灯光控制任务），
 * 后台自动到点执行开灯/关灯。
 *
 * 灯光控制任务在新增/编辑时，会同步到照明计划表（lighting_plan + lighting_plan_execution_time），
 * 保证 /bems/lighting/plan/listPage 计划列表能看到这些定时任务。
 *
 * 灯光控制示例（用户友好时间配置，与照明计划接口一致）：
 *   POST /bems/scheduleJob/add
 *   {
 *     "jobName": "金安桥A区晚上开灯",
 *     "relType": "区域",            // 区域 / 回路
 *     "relIds": "1,2,3",            // 多目标ID，逗号分隔
 *     "operationType": "OPEN",      // OPEN-开灯 CLOSE-关灯
 *     "planType": "普通计划",        // 普通计划 / 节日计划 / 应急计划
 *     "executionTime": "18:00:00",  // 执行时间 HH:mm:ss（不用写 cron！）
 *     "cycleType": "每天",           // 每天 / 工作日 / 周末 / 自定义
 *     "startDate": "2026-08-01",    // 生效开始日期（可选）
 *     "endDate": "2026-12-31",      // 生效结束日期（可选）
 *     "remark": "每天晚上6点开灯"
 *   }
 *
 * 自定义周期（指定周几执行）：
 *   {
 *     "jobName": "周一三五关灯",
 *     "relType": "回路",
 *     "relIds": "5,6",
 *     "operationType": "CLOSE",
 *     "executionTime": "23:00:00",
 *     "cycleType": "自定义",
 *     "enabledWeek": "1,3,5",       // 1=周一 2=周二 ... 7=周日
 *     "startDate": "2026-08-01",
 *     "endDate": "2026-12-31"
 *   }
 *
 * 高级用户也可直接传 cronExpression（executionTime 为空时生效）：
 *   {
 *     "jobName": "每天凌晨2点生成报告",
 *     "beanName": "energyDataSyncJob",
 *     "methodName": "calculateMeteringPointData",
 *     "cronExpression": "0 0 2 * * ?"
 *   }
 *
 * 兼容旧写法（单目标）：
 *   {
 *     "jobName": "金安桥A区晚上开灯",
 *     "controlType": "AREA",      // AREA-区域 CIRCUIT-回路
 *     "targetId": 1,
 *     "operationType": "OPEN",
 *     "cronExpression": "0 0 18 * * ?"
 *   }
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/bems/scheduleJob")
@Api(tags = "动态定时任务管理")
public class ScheduleJobController {

    private final IScheduleJobService scheduleJobService;

    private final DynamicScheduleManager dynamicScheduleManager;

    private final ILightingPlanService lightingPlanService;

    /** 合法的周期类型 */
    private static final List<String> VALID_CYCLE_TYPES = Arrays.asList("每天", "工作日", "周末", "自定义");

    /**
     * 分页列表查询
     */
    @GetMapping("/list")
    @ApiOperation(value = "定时任务列表", notes = "分页查询定时任务")
    public Result<IPage<ScheduleJob>> list(ScheduleJob params) {
        Page<ScheduleJob> page = new Page<>(params.getPageNo(), params.getPageSize());
        LambdaQueryWrapper<ScheduleJob> queryWrapper = new LambdaQueryWrapper<ScheduleJob>()
                .orderByDesc(ScheduleJob::getCreateTime);
        IPage<ScheduleJob> pageList = scheduleJobService.page(page, queryWrapper);
        return Result.ok(pageList);
    }

    /**
     * 判断是否为灯光控制模式
     * 兼容新版（relType=区域/回路）与旧版（controlType=AREA/CIRCUIT）
     */
    private boolean isLightingControlJob(ScheduleJob scheduleJob) {
        return "AREA".equals(scheduleJob.getControlType())
                || "CIRCUIT".equals(scheduleJob.getControlType())
                || "区域".equals(scheduleJob.getRelType())
                || "回路".equals(scheduleJob.getRelType());
    }

    /**
     * 参数校验（灯光控制 / 通用模式 + 时间配置）
     */
    private Result<String> validateJob(ScheduleJob scheduleJob) {
        if (isLightingControlJob(scheduleJob)) {
            // 灯光控制模式：需要目标ID（targetId 单目标 或 relIds 多目标）
            if (scheduleJob.getTargetId() == null && StringUtils.isBlank(scheduleJob.getRelIds())) {
                return Result.error("灯光控制模式必须指定 targetId（单目标）或 relIds（多目标，逗号分隔）");
            }
            if (scheduleJob.getOperationType() == null ||
                    (!"OPEN".equals(scheduleJob.getOperationType()) && !"CLOSE".equals(scheduleJob.getOperationType()))) {
                return Result.error("operationType 必须为 OPEN（开灯）或 CLOSE（关灯）");
            }
        } else {
            // 通用反射模式校验
            if (scheduleJob.getBeanName() == null || scheduleJob.getBeanName().trim().isEmpty()) {
                return Result.error("通用模式下 Spring Bean 名称不能为空");
            }
            if (scheduleJob.getMethodName() == null || scheduleJob.getMethodName().trim().isEmpty()) {
                return Result.error("通用模式下执行方法名不能为空");
            }
        }
        return null; // 校验通过
    }

    /**
     * 校验时间配置：executionTime 或 cronExpression 至少填一个；executionTime 必须为合法的 HH:mm:ss
     */
    private Result<String> validateTimeConfig(ScheduleJob scheduleJob) {
        boolean hasCron = StringUtils.isNotBlank(scheduleJob.getCronExpression());
        boolean hasExecutionTime = StringUtils.isNotBlank(scheduleJob.getExecutionTime());
        if (!hasCron && !hasExecutionTime) {
            return Result.error("请填写执行时间 executionTime（如 18:00:00）或 cronExpression（高级）");
        }
        if (hasExecutionTime && !scheduleJob.getExecutionTime().matches("^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$")) {
            return Result.error("executionTime 格式错误，应为 HH:mm:ss（时 00-23，分秒 00-59），如 18:00:00");
        }
        // 周期类型值域校验
        if (StringUtils.isNotBlank(scheduleJob.getCycleType())
                && !VALID_CYCLE_TYPES.contains(scheduleJob.getCycleType())) {
            return Result.error("cycleType 取值仅支持：每天、工作日、周末、自定义");
        }
        // 周期类型为自定义时必须填 enabledWeek，且每项必须为 1-7
        if ("自定义".equals(scheduleJob.getCycleType())) {
            if (StringUtils.isBlank(scheduleJob.getEnabledWeek())) {
                return Result.error("周期类型为自定义时必须填写 enabledWeek（1-7，1=周一，多个逗号分隔）");
            }
            boolean allValid = Arrays.stream(scheduleJob.getEnabledWeek().split(","))
                    .map(String::trim)
                    .allMatch(s -> s.matches("[1-7]"));
            if (!allValid) {
                return Result.error("enabledWeek 每项取值必须为 1-7（1=周一，7=周日），多个用逗号分隔");
            }
        }
        return null;
    }

    /**
     * 生成并回填 cron 表达式
     *
     * 规则：只要 executionTime 非空，就以它为权威来源，总是用 buildCron 重新生成 cron。
     * 注意：重建前必须先把 cronExpression 置空，因为 buildCron 对非空 cronExpression 会直接原样返回。
     * 如果不置空，前端编辑时回传的旧 cronExpression 会覆盖新生成的值，导致改了 executionTime 也不刷新（陈旧 cron bug）。
     * 仅当 executionTime 为空（高级模式，只填 cronExpression）时保留传入的 cronExpression。
     */
    private void fillCronExpression(ScheduleJob scheduleJob) {
        if (StringUtils.isNotBlank(scheduleJob.getExecutionTime())) {
            scheduleJob.setCronExpression(null);
            scheduleJob.setCronExpression(dynamicScheduleManager.buildCron(scheduleJob));
        }
    }

    /**
     * 添加定时任务
     *
     * 灯光控制任务必填: relType(或controlType), relIds(或targetId), operationType, 时间配置
     * 通用任务必填: beanName, methodName, 时间配置
     */
    @PostMapping("/add")
    @ApiOperation(value = "添加定时任务",
            notes = "新增定时任务。灯光控制: 传relType+relIds+operationType。通用反射: 传beanName+methodName。时间: executionTime+cycleType 或 cronExpression")
    public Result<String> add(@RequestBody ScheduleJob scheduleJob) {
        if (scheduleJob.getJobName() == null || scheduleJob.getJobName().trim().isEmpty()) {
            return Result.error("任务名称不能为空");
        }

        // 校验时间配置（executionTime / cronExpression 二选一）
        Result<String> timeConfig = validateTimeConfig(scheduleJob);
        if (timeConfig != null) {
            return timeConfig;
        }

        // 校验字段
        Result<String> validation = validateJob(scheduleJob);
        if (validation != null) {
            return validation;
        }

        // 自动生成 cron 并随任务落库
        fillCronExpression(scheduleJob);

        // 默认启用
        if (scheduleJob.getStatus() == null) {
            scheduleJob.setStatus(1);
        }

        scheduleJobService.save(scheduleJob);

        if (scheduleJob.getStatus() == 1) {
            try {
                scheduleJobService.start(scheduleJob.getId());
            } catch (Exception e) {
                log.error("定时任务添加成功但注册失败: {}", e.getMessage());
                // 同步计划状态回禁用，避免与调度器实际状态不一致
                lightingPlanService.syncStatusFromScheduleJob(scheduleJob.getId(), 0);
                return Result.error("定时任务已保存，但注册到调度器失败: " + e.getMessage());
            }
        }

        // 同步到照明计划表（lighting_plan + lighting_plan_execution_time），保证计划列表可查
        try {
            lightingPlanService.syncFromScheduleJob(scheduleJob);
        } catch (Exception e) {
            log.error("定时任务同步到照明计划表失败: {}", e.getMessage());
        }

        return Result.ok("添加成功");
    }

    /**
     * 编辑定时任务
     */
    @PutMapping("/edit")
    @ApiOperation(value = "编辑定时任务", notes = "更新定时任务配置，并刷新调度器")
    public Result<String> edit(@RequestBody ScheduleJob scheduleJob) {
        if (scheduleJob.getId() == null) {
            return Result.error("id 不能为空");
        }

        // 校验时间配置
        Result<String> timeConfig = validateTimeConfig(scheduleJob);
        if (timeConfig != null) {
            return timeConfig;
        }

        // 校验字段
        Result<String> validation = validateJob(scheduleJob);
        if (validation != null) {
            return validation;
        }

        // 自动生成 cron 并随任务落库（编辑改了执行时间/周期/周几时自动刷新，避免陈旧 cron）
        fillCronExpression(scheduleJob);

        scheduleJobService.updateById(scheduleJob);

        if (scheduleJob.getStatus() == 1) {
            scheduleJobService.start(scheduleJob.getId());
        } else {
            scheduleJobService.stop(scheduleJob.getId());
        }

        // 同步到照明计划表
        try {
            lightingPlanService.syncFromScheduleJob(scheduleJobService.getById(scheduleJob.getId()));
        } catch (Exception e) {
            log.error("定时任务同步到照明计划表失败: {}", e.getMessage());
        }

        return Result.ok("更新成功");
    }

    /**
     * 删除定时任务
     */
    @DeleteMapping("/delete")
    @ApiOperation(value = "删除定时任务", notes = "删除定时任务，并从调度器中移除，同时删除同步的照明计划")
    public Result<String> delete(@RequestParam(name = "id") Long id) {
        // 任务可能已被计划接口先删除，判空避免 stop 抛异常
        if (scheduleJobService.getById(id) != null) {
            scheduleJobService.stop(id);
            scheduleJobService.removeById(id);
        }
        // 删除同步的照明计划
        lightingPlanService.removeByScheduleJobId(id);
        return Result.ok("删除成功");
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/deleteBatch")
    @ApiOperation(value = "批量删除定时任务")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        for (String id : idList) {
            Long jobId = Long.parseLong(id);
            if (scheduleJobService.getById(jobId) != null) {
                scheduleJobService.stop(jobId);
                scheduleJobService.removeById(jobId);
            }
            lightingPlanService.removeByScheduleJobId(jobId);
        }
        return Result.ok("批量删除成功");
    }

    /**
     * 启用定时任务
     */
    @PostMapping("/start/{id}")
    @ApiOperation(value = "启用定时任务", notes = "启用指定定时任务，注册到调度器，并同步照明计划状态")
    public Result<String> start(@PathVariable Long id) {
        scheduleJobService.start(id);
        lightingPlanService.syncStatusFromScheduleJob(id, 1);
        return Result.ok("启用成功");
    }

    /**
     * 停用定时任务
     */
    @PostMapping("/stop/{id}")
    @ApiOperation(value = "停用定时任务", notes = "停用指定定时任务，从调度器移除，并同步照明计划状态")
    public Result<String> stop(@PathVariable Long id) {
        scheduleJobService.stop(id);
        lightingPlanService.syncStatusFromScheduleJob(id, 0);
        return Result.ok("停用成功");
    }

    /**
     * 手动执行一次
     */
    @PostMapping("/execute/{id}")
    @ApiOperation(value = "手动执行一次", notes = "立即执行一次定时任务，不影响原有调度计划")
    public Result<String> execute(@PathVariable Long id) {
        scheduleJobService.executeOnce(id);
        return Result.ok("执行成功");
    }

    /**
     * 获取单个任务详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取任务详情")
    public Result<ScheduleJob> getById(@PathVariable Long id) {
        ScheduleJob job = scheduleJobService.getById(id);
        if (job == null) {
            return Result.error("任务不存在");
        }
        return Result.ok(job);
    }
}
