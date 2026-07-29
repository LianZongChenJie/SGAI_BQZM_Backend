package org.jeecg.modules.bems.event.controller;

import com.alibaba.cloud.commons.lang.StringUtils;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.bems.alarm.service.IAlarmRecordService;
import org.jeecg.modules.bems.event.dto.*;
import org.jeecg.modules.bems.event.service.EventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 事件工单相关
 */
@AllArgsConstructor
@RestController
@RequestMapping("/bems/event")
public class EventController {
    private final EventService eventService;

    private final IAlarmRecordService alarmRecordService;

    /**
     * 获取事件详情
     * @param eventId 事件id
     * @return 事件详情、事件记录
     */
    @GetMapping("/getDetail")
    public Result<EventDetail> getDetail(@RequestParam("eventId") String eventId){
        Event event = eventService.getEventDetail(eventId);
        List<EventOperateRecord> eventRecord = eventService.getEventRecord(eventId);
        return Result.ok(new EventDetail(event, eventRecord));
    }

    /**
     * 获取空间信息
     * @return 空间信息
     */
    @GetMapping("/getEventSpace")
    public Result<List<EventSpace>> getEventSpace(){
        List<EventSpace> eventSpace = eventService.getEventSpace();
        return Result.ok(eventSpace);
    }

    /**
     * 事件分布
     */
    @GetMapping("/eventDistribution")
    public Result<?> eventDistribution(){
        return Result.ok(eventService.eventDistribution());
    }

    /**
     * 工单分布
     */
    @GetMapping("/orderDistribution")
    public Result<?> orderDistribution(){
        return Result.ok(eventService.orderDistribution());
    }

    @IgnoreAuth
    @PostMapping("/updateStatus")
    public Result<String> updateStatus(@RequestBody UpdateStatusDto param){
        // 待处理、已转工单、待评价、待确认、已完成
        if(StringUtils.isEmpty(param.getId()) || StringUtils.isEmpty(param.getStatus())){
            return Result.error("参数错误");
        }
        if("已完成".equals(param.getStatus())){
            alarmRecordService.completed(param.getId());
        }
        return Result.ok();
    }
}
