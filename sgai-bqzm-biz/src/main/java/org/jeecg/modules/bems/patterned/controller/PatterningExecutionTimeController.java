package org.jeecg.modules.bems.patterned.controller;

import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.patterned.entity.PatterningExecutionTime;
import org.jeecg.modules.bems.patterned.service.IPatterningExecutionTimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bems/patterningExecutionTime")
@AllArgsConstructor
public class PatterningExecutionTimeController {

    private final IPatterningExecutionTimeService service;

    @GetMapping("/getById")
    public Result<PatterningExecutionTime> getByPatterningIdId(@RequestParam Long patterningId){
        return Result.ok(service.getByPatterningId(patterningId));
    }
}
