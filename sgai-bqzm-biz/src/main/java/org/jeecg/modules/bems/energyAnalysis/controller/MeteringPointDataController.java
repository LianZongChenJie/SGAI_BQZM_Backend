package org.jeecg.modules.bems.energyAnalysis.controller;

import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.energyAnalysis.dto.MeteringPointChatDto;
import org.jeecg.modules.bems.energyAnalysis.dto.MeteringPointDataDto;
import org.jeecg.modules.bems.energyAnalysis.dto.RecalculateDto;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataService;
import org.jeecg.modules.bems.energyAnalysis.vo.Chat;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointDataChartVo;
import org.jeecg.modules.bems.energyAnalysis.vo.chat.PieChat;
import org.jeecg.modules.bems.mq.send.MqSendService;
import org.springframework.web.bind.annotation.*;


@Api(tags = "计量点位数据信息")
@RestController
@RequestMapping("/bems/meterPointData")
@AllArgsConstructor
public class MeteringPointDataController {

    private final IMeteringPointDataService service;

    private final MqSendService mqSendService;

    @GetMapping("/findMinute")
    public Result<MeteringPointDataChartVo> findMinute(MeteringPointDataDto param){
        return Result.ok(new MeteringPointDataChartVo(service.findMinute(param.getEnergyFlowDiagramIds(), param.getHour())));
    }

    @ApiOperation(value = "查询日数据", notes = "查询日数据")
    @GetMapping("/findDay")
    public Result<MeteringPointDataChartVo> findDay(MeteringPointDataDto param) {
        return Result.ok(new MeteringPointDataChartVo(service.findDay(param.getEnergyFlowDiagramIds(), param.getDay())));
    }

    @ApiOperation(value = "查询月数据", notes = "查询月数据")
    @GetMapping("/findMonth")
    public Result<MeteringPointDataChartVo> findMonth(MeteringPointDataDto param){
        return Result.ok(new MeteringPointDataChartVo(service.findMonth(param.getEnergyFlowDiagramIds(), param.getDay())));
    }

    @ApiOperation(value = "查询年数据", notes = "查询年数据")
    @GetMapping("/findYear")
    public Result<MeteringPointDataChartVo> findYear(MeteringPointDataDto param){
        return Result.ok(new MeteringPointDataChartVo(service.findYear(param.getEnergyFlowDiagramIds(), param.getDay())));
    }

    @ApiOperation(value = "重新计算", notes = "重新计算")
    @AutoLog(value = "计量点位-重新计算")
    @RequiresPermissions("bems:meterPointData:calculateValue")
    @PostMapping("/calculateValue")
    public Result<String> calculateValue(@RequestBody MeteringPointDataDto param){
        service.calculateValue(param.getHour());
        return Result.ok("计算成功");
    }

    /**
     * 重新计算，批量
     * @param param
     * @return
     */
    @PostMapping("/recalculate")
    public Result<String> recalculate(@RequestBody RecalculateDto param){
        if(param.getTime() == null || StrUtil.isEmpty(param.getMeteringPointIds())){
            return Result.error("参数错误");
        }
        String[] split = param.getMeteringPointIds().split(",");
        for(String id : split) {
            mqSendService.sendMeteringPointValueUpdate(Long.valueOf(id),param.getTime());
        }
        return Result.ok();
    }

    /**
     * 饼图
     */
    @GetMapping("/findPieChat")
    public Result<PieChat> findPieChat(MeteringPointChatDto param){
        return Result.ok(service.findPieChat(param));
    }

    /**
     * 折线图
     */
    @GetMapping("/findLineChat")
    public Result<Chat> findLineChat(MeteringPointChatDto param){
        return Result.ok(service.findLineChat(param));
    }

    /**
     * 柱状图
     */
    @GetMapping("/findBarChat")
    public Result<Chat> findBarChat(MeteringPointChatDto param){
        return Result.ok(service.findBarChat(param));
    }

    /**
     * 堆叠柱状图
     */
    @GetMapping("/findStackedColumnChart")
    public Result<Chat> findStackedColumnChart(MeteringPointChatDto param){
        return Result.ok(service.findStackedColumnChart(param));
    }

}
