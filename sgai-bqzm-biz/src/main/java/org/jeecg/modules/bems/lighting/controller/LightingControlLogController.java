package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingControlLog;
import org.jeecg.modules.bems.lighting.service.ILightingControlLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制日志
 */
@Api(tags = "照明-控制日志")
@RestController
@RequestMapping("/bems/lighting/controlLog")
@AllArgsConstructor
public class LightingControlLogController {

    private final ILightingControlLogService service;

    /**
     * 分页查询控制日志
     */
    @ApiOperation("分页查询控制日志")
    @GetMapping("/listPage")
    public Result<?> listPage(LightingControlLog params,
                              @RequestParam(defaultValue = "1") int pageNo,
                              @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(service.listPage(params, pageNo, pageSize));
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
