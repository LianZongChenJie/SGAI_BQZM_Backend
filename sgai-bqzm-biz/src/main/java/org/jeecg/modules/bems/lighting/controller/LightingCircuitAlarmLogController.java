package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitAlarmLog;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitAlarmLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 照明回路报警历史流水
 */
@Api(tags = "照明-报警历史流水")
@Slf4j
@RestController
@RequestMapping("/bems/lighting/alarmLog")
@AllArgsConstructor
public class LightingCircuitAlarmLogController {

    private final ILightingCircuitAlarmLogService alarmLogService;

    @ApiOperation("分页查询报警历史流水")
    @GetMapping("/page")
    public Result<IPage<LightingCircuitAlarmLog>> page(
            @ApiParam("页码") @RequestParam(defaultValue = "1") long pageNo,
            @ApiParam("每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @ApiParam("回路名称模糊") @RequestParam(required = false) String circuitName,
            @ApiParam("区域ID") @RequestParam(required = false) Long areaId,
            @ApiParam("命中规则编码 R1/R2") @RequestParam(required = false) String ruleCode,
            @ApiParam("状态：报警中/已恢复") @RequestParam(required = false) String status,
            @ApiParam("报警开始时间起") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("报警开始时间止") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<LightingCircuitAlarmLog> page = alarmLogService.pageQuery(
                new Page<>(pageNo, pageSize), circuitName, areaId, ruleCode, status, startTime, endTime);
        return Result.ok(page);
    }
}
