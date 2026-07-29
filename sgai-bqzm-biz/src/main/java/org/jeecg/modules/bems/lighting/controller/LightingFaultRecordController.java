package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingFaultRecord;
import org.jeecg.modules.bems.lighting.service.ILightingFaultRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 故障记录
 */
@Api(tags = "照明-故障记录")
@RestController
@RequestMapping("/bems/lighting/faultRecord")
@AllArgsConstructor
public class LightingFaultRecordController {

    private final ILightingFaultRecordService service;

    /**
     * 分页查询故障记录
     */
    @ApiOperation("分页查询故障记录")
    @GetMapping("/listPage")
    public Result<?> listPage(LightingFaultRecord params,
                              @RequestParam(defaultValue = "1") int pageNo,
                              @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(service.listPage(params, pageNo, pageSize));
    }

    /**
     * 获取故障详情
     */
    @ApiOperation("获取故障详情")
    @GetMapping("/detail")
    public Result<?> detail(Long id) {
        return Result.ok(service.getById(id));
    }

    /**
     * 故障类型统计
     */
    @ApiOperation("故障类型统计")
    @GetMapping("/countByFaultType")
    public Result<?> countByFaultType(
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(service.countByFaultType(startTime, endTime));
    }

    /**
     * 故障趋势统计
     */
    @ApiOperation("故障趋势统计")
    @GetMapping("/countByDate")
    public Result<?> countByDate(
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(service.countByDate(startTime, endTime));
    }

    /**
     * 新增故障记录
     */
    @ApiOperation("新增故障记录")
    @PostMapping("/add")
    public Result<?> add(@RequestBody LightingFaultRecord faultRecord) {
        service.save(faultRecord);
        return Result.ok("新增成功");
    }

    /**
     * 更新故障记录
     */
    @ApiOperation("更新故障记录")
    @PostMapping("/update")
    public Result<?> update(@RequestBody LightingFaultRecord faultRecord) {
        service.updateById(faultRecord);
        return Result.ok("更新成功");
    }

    /**
     * 删除故障记录
     */
    @ApiOperation("删除故障记录")
    @PostMapping("/delete")
    public Result<?> delete(Long id) {
        service.removeById(id);
        return Result.ok("删除成功");
    }
}
