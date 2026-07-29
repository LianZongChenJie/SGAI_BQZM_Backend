package org.jeecg.modules.bems.lighting.controller;

import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecutionTimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bems/lighting/planExecutionTime")
@AllArgsConstructor
public class LightingPlanExecutionTimeController {

    private final ILightingPlanExecutionTimeService service;

    @GetMapping("/getByPlanId")
    public Result<?> getByPlanId(Long planId){
        return Result.ok(service.getByPlanId(planId));
    }

}
