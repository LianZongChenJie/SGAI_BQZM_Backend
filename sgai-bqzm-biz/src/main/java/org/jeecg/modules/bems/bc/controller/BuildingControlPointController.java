package org.jeecg.modules.bems.bc.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.bems.bc.entity.BuildingControlPoint;
import org.jeecg.modules.bems.bc.service.IBuildingControlPointService;
import org.jeecg.modules.bems.mq.send.MqSendService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 楼控点位数据
 */
@RestController
@RequestMapping("/bems/bc/buildingControlPoint")
@AllArgsConstructor
public class BuildingControlPointController {

    private final IBuildingControlPointService service;

    private final MqSendService mqSendService;

    @GetMapping("/listPage")
    public Result<Page<BuildingControlPoint>> listPage(BuildingControlPoint params){
        return Result.ok(service.listPage(params));
    }

    @IgnoreAuth
    @PostMapping("/save")
    public Result<String> save(@RequestBody BuildingControlPoint entity){
        service.save(entity.getGatewayAdr(),entity.getBacnetAdr(),entity.getValue(),entity.getContent(), LocalDateTime.now());
        return Result.ok();
    }

    @PostMapping("/control")
    public Result<String> control(@RequestBody BuildingControlPoint entity){
        mqSendService.sendBuildingControlOperation(entity.getGatewayAdr(),entity.getBacnetAdr(),entity.getValue());
        return Result.ok();
    }
}
