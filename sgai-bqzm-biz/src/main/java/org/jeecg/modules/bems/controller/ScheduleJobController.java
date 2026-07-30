package org.jeecg.modules.bems.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.entity.ScheduleJob;
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
 * 灯光控制示例：
 *   POST /bems/scheduleJob/add
 *   {
 *     "jobName": "金安桥A区晚上开灯",
 *     "controlType": "AREA",
 *     "targetId": 1,
 *     "operationType": "OPEN",
 *     "cronExpression": "0 0 18 * * ?",
 *     "remark": "每天晚上6点开灯"
 *   }
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/bems/scheduleJob")
@Api(tags = "动态定时任务管理")
public class ScheduleJobController {

    private final IScheduleJobService scheduleJobService;

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
     * 参数校验（灯光控制 / 通用模式）
     */
    private Result<String> validateJob(ScheduleJob scheduleJob) {
        if ("AREA".equals(scheduleJob.getControlType()) || "CIRCUIT".equals(scheduleJob.getControlType())) {
            if (scheduleJob.getTargetId() == null) {
                return Result.error("灯光控制模式必须指定 targetId（区域ID或回路ID）");
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
     * 添加定时任务
     *
     * 灯光控制任务必填: controlType, targetId, operationType
     * 通用任务必填: beanName, methodName
     */
    @PostMapping("/add")
    @ApiOperation(value = "添加定时任务",
            notes = "新增定时任务。灯光控制: 传controlType+targetId+operationType。通用反射: 传beanName+methodName")
    public Result<String> add(@RequestBody ScheduleJob scheduleJob) {
        if (scheduleJob.getJobName() == null || scheduleJob.getJobName().trim().isEmpty()) {
            return Result.error("任务名称不能为空");
        }
        if (scheduleJob.getCronExpression() == null || scheduleJob.getCronExpression().trim().isEmpty()) {
            return Result.error("cron 表达式不能为空");
        }

        // 校验字段
        Result<String> validation = validateJob(scheduleJob);
        if (validation != null) {
            return validation;
        }

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
                return Result.error("定时任务已保存，但注册到调度器失败: " + e.getMessage());
            }
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

        // 校验字段
        Result<String> validation = validateJob(scheduleJob);
        if (validation != null) {
            return validation;
        }

        scheduleJobService.updateById(scheduleJob);

        if (scheduleJob.getStatus() == 1) {
            scheduleJobService.start(scheduleJob.getId());
        } else {
            scheduleJobService.stop(scheduleJob.getId());
        }

        return Result.ok("更新成功");
    }

    /**
     * 删除定时任务
     */
    @DeleteMapping("/delete")
    @ApiOperation(value = "删除定时任务", notes = "删除定时任务，并从调度器中移除")
    public Result<String> delete(@RequestParam(name = "id") Long id) {
        scheduleJobService.stop(id);
        scheduleJobService.removeById(id);
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
            scheduleJobService.stop(jobId);
            scheduleJobService.removeById(jobId);
        }
        return Result.ok("批量删除成功");
    }

    /**
     * 启用定时任务
     */
    @PostMapping("/start/{id}")
    @ApiOperation(value = "启用定时任务", notes = "启用指定定时任务，注册到调度器")
    public Result<String> start(@PathVariable Long id) {
        scheduleJobService.start(id);
        return Result.ok("启用成功");
    }

    /**
     * 停用定时任务
     */
    @PostMapping("/stop/{id}")
    @ApiOperation(value = "停用定时任务", notes = "停用指定定时任务，从调度器移除")
    public Result<String> stop(@PathVariable Long id) {
        scheduleJobService.stop(id);
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
