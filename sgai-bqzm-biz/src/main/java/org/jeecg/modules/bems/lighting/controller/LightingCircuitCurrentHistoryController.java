package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingCircuitCurrentHistory;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitCurrentHistoryService;
import org.jeecg.modules.bems.permission.annotation.DataPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 回路电流采样历史查询接口（2026-11-12 新增）
 */
@Api(tags = "照明-回路电流历史")
@RestController
@RequestMapping("/bems/lighting/circuitCurrent")
@AllArgsConstructor
public class LightingCircuitCurrentHistoryController {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DF_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ILightingCircuitCurrentHistoryService historyService;

    @ApiOperation("回路电流整点采样历史（按回路/区域/时间范围）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "circuitId", value = "回路ID", paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "areaId", value = "区域ID", paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "start", value = "开始时间(yyyy-MM-dd HH:mm:ss)", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "end", value = "结束时间(yyyy-MM-dd HH:mm:ss)", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "limit", value = "最大条数(默认500，上限5000)", paramType = "query", dataType = "int")
    })
    @DataPermission
    @GetMapping("/history")
    public Result<List<LightingCircuitCurrentHistory>> history(@RequestParam(required = false) Long circuitId,
                                                               @RequestParam(required = false) Long areaId,
                                                               @RequestParam(required = false) String start,
                                                               @RequestParam(required = false) String end,
                                                               @RequestParam(required = false) Integer limit) {
        try {
            LocalDateTime s = parse(start, false);
            LocalDateTime e = parse(end, true);
            return Result.ok(historyService.queryHistory(circuitId, areaId, s, e, limit));
        } catch (Exception ex) {
            return Result.error("时间格式错误，需 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd");
        }
    }

    @ApiOperation("手工补采一次电流快照（取当前整点；同一整点重复调用不会重复写）")
    @PostMapping("/snapshot")
    public Result<Integer> snapshot() {
        int saved = historyService.snapshotNow();
        return Result.ok(saved);
    }

    /**
     * 时间解析：支持 yyyy-MM-dd HH:mm:ss 与 yyyy-MM-dd；纯日期作为结束时间时补到当天 23:59:59
     */
    private LocalDateTime parse(String text, boolean isEnd) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String v = text.trim();
        if (v.length() <= 10) {
            LocalDate d = LocalDate.parse(v, DF_DAY);
            return isEnd ? d.atTime(23, 59, 59) : d.atStartOfDay();
        }
        return LocalDateTime.parse(v, DF);
    }
}
