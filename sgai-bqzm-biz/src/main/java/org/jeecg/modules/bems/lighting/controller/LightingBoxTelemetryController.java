package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetry;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetryHistory;
import org.jeecg.modules.bems.lighting.service.ILightingBoxTelemetryService;
import org.jeecg.modules.bems.lighting.vo.BoxTreeVo;
import org.jeecg.modules.bems.permission.annotation.DataPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 箱子遥测查询接口
 */
@Api(tags = "照明-箱子遥测")
@RestController
@RequestMapping("/bems/lighting/boxTelemetry")
@AllArgsConstructor
public class LightingBoxTelemetryController {

    private final ILightingBoxTelemetryService boxTelemetryService;

    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @ApiOperation("箱子列表(最新快照)")
    @DataPermission
    @GetMapping("/list")
    public Result<List<LightingBoxTelemetry>> list() {
        return Result.ok(boxTelemetryService.listBoxes());
    }

    @ApiOperation("片区-区域-箱子 三级树结构")
    @DataPermission
    @GetMapping("/boxTree")
    public Result<List<BoxTreeVo>> boxTree() {
        return Result.ok(boxTelemetryService.boxTree());
    }

    @ApiOperation("箱子历史数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "gatewayCode", value = "箱子编号", paramType = "query", required = true, dataType = "string"),
            @ApiImplicitParam(name = "start", value = "开始时间(yyyy-MM-dd HH:mm:ss)", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "end", value = "结束时间(yyyy-MM-dd HH:mm:ss)", paramType = "query", dataType = "string")
    })
    @DataPermission
    @GetMapping("/history")
    public Result<List<LightingBoxTelemetryHistory>> history(@RequestParam String gatewayCode,
                                                             @RequestParam(required = false) String start,
                                                             @RequestParam(required = false) String end) {
        try {
            Date s = start == null || start.isEmpty() ? null : DF.parse(start);
            Date e = end == null || end.isEmpty() ? null : DF.parse(end);
            return Result.ok(boxTelemetryService.history(gatewayCode, s, e));
        } catch (ParseException ex) {
            return Result.error("时间格式错误，需 yyyy-MM-dd HH:mm:ss");
        }
    }
}