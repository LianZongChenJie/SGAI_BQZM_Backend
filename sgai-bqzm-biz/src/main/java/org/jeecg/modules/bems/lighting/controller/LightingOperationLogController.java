package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingOperationLogQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * 照明控制记录（控制日志）
 */
@Api(tags = "照明-控制日志")
@RestController
@RequestMapping("/bems/lighting/operationLog")
@AllArgsConstructor
public class LightingOperationLogController {

    private final ILightingOperationLogService service;

    /**
     * 分页查询控制日志
     * 支持按日志类型（场景/定时任务/区域/回路）、关联类型（区域/回路）、操作类型（开/关）、操作时间段（startTime~endTime）筛选
     * 默认只查顶层日志（parentId为null）
     */
    @ApiOperation("分页查询控制日志（支持按日志类型、关联类型、操作类型、操作时间段筛选）")
    @GetMapping("/listPage")
    public Result<IPage<LightingOperationLog>> listPage(LightingOperationLogQueryDto param) {
        return Result.ok(service.listPage(param));
    }

    /**
     * 导出控制日志Excel（查询条件同 listPage，不分页）
     */
    @ApiOperation("导出控制日志Excel")
    @GetMapping("/export")
    public void export(LightingOperationLogQueryDto param, HttpServletResponse response) {
        service.exportExcel(param, response);
    }

    /**
     * 查询控制日志详情（包含子日志列表）
     * 场景日志详情：返回场景下的区域/回路子日志
     * 定时任务日志详情：返回定时任务执行的区域/回路子日志
     * 区域日志详情：返回区域下的回路子日志
     * 回路日志详情：返回回路本身（无子日志）
     */
    @ApiOperation("查询控制日志详情（包含子日志列表）")
    @GetMapping("/detail")
    public Result<LightingOperationLog> detail(Long id) {
        return Result.ok(service.getDetail(id));
    }
}
