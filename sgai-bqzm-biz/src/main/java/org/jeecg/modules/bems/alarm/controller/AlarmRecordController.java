package org.jeecg.modules.bems.alarm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.alarm.dto.AlarmRecordDto;
import org.jeecg.modules.bems.alarm.dto.TransferEventDto;
import org.jeecg.modules.bems.alarm.entity.AlarmRecord;
import org.jeecg.modules.bems.alarm.service.IAlarmRecordService;
import org.jeecg.modules.bems.alarm.vo.AlarmRecordStatisticsVo;
import org.jeecg.modules.bems.mdm.entity.DeviceAttribute;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警记录
 */
@Api(tags = "告警管理-告警记录")
@RestController
@RequestMapping("/bems/alarm/record")
@AllArgsConstructor
public class AlarmRecordController {

    private final IAlarmRecordService service;

    @ApiOperation("分页查询告警记录")
    @GetMapping("/listPage")
    public Result<IPage<AlarmRecord>> listPage(AlarmRecordDto params){
        return Result.ok(service.listPage(params));
    }

    @ApiOperation("消除告警")
    @PostMapping("/elimination")
    @RequiresPermissions("bems:alarmRecord:elimination")
    @AutoLog(value = "告警记录-消除")
    public Result<String> elimination(@RequestParam(name = "id") Long id){
        service.elimination(id);
        return Result.ok();
    }

    @ApiOperation("批量消除告警")
    @PostMapping("/eliminations")
    @AutoLog(value = "告警记录-批量消除")
    public Result<String> eliminations(@RequestBody List<Long> ids){
        ids.forEach(service::elimination);
        return Result.ok();
    }

    /**
     * 告警级别统计
     */
    @ApiOperation("告警级别统计")
    @GetMapping("/levelStatistics")
    public Result<List<AlarmRecordStatisticsVo>> levelStatistics(AlarmRecordDto params){
        return Result.ok(service.levelStatistics(params));
    }

    @ApiOperation("测试告警检测")
    @GetMapping("/test")
    public Result<String> test(DeviceAttribute attribute){
        service.alarmDetection(attribute.getDeviceId(),attribute.getId(),attribute.getValue());
        return Result.ok();
    }

    /**
     * 转事件工单
     * @return
     */
    @ApiOperation("转事件工单")
    @PostMapping("/transferEvent")
    public Result<String> transferEvent(@RequestBody TransferEventDto data){
        service.transferEvent(data);
        return Result.ok();
    }
}
