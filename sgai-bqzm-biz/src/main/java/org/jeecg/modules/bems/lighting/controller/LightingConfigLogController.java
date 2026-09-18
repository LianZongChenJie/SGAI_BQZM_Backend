package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingConfigLogQueryDto;
import org.jeecg.modules.bems.lighting.service.ILightingConfigLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置日志
 */
@Api(tags = "照明-配置日志")
@RestController
@RequestMapping("/bems/lighting/configLog")
@AllArgsConstructor
public class LightingConfigLogController {

    private final ILightingConfigLogService service;

    /**
     * 分页查询配置日志
     * 支持：操作类型/操作模块/操作人账号/操作对象类型/操作对象ID（精确），
     * 操作人姓名/操作对象名称/操作内容（模糊），操作时间段 startTime~endTime（含边界）
     */
    @ApiOperation("分页查询配置日志（支持操作类型、模块、操作人、对象、内容关键字、操作时间段筛选）")
    @GetMapping("/listPage")
    public Result<?> listPage(LightingConfigLogQueryDto param) {
        return Result.ok(service.listPage(param));
    }

    /**
     * 获取日志详情
     */
    @ApiOperation("获取日志详情")
    @GetMapping("/detail")
    public Result<?> detail(Long id) {
        return Result.ok(service.getById(id));
    }
}
