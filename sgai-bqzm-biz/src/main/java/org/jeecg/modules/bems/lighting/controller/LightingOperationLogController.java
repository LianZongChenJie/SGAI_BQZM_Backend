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
     * 支持按关联类型（区域/回路）、操作类型（开/关）、操作时间段（startTime~endTime）筛选
     */
    @ApiOperation("分页查询控制日志（支持按关联类型、操作类型、操作时间段筛选）")
    @GetMapping("/listPage")
    public Result<IPage<LightingOperationLog>> listPage(LightingOperationLogQueryDto param) {
        return Result.ok(service.listPage(param));
    }
}
