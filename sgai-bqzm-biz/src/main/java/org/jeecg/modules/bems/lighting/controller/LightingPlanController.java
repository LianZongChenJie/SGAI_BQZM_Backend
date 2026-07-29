package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingPlanDetailDto;
import org.jeecg.modules.bems.lighting.dto.LightingPlanQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.springframework.web.bind.annotation.*;

/**
 * 照明计划
 */
@RestController
@AllArgsConstructor
@RequestMapping("/bems/lighting/plan")
public class LightingPlanController {

    private final ILightingPlanService service;


    @GetMapping("/listPage")
    public Result<IPage<LightingPlan>> listPage(LightingPlanQueryDto params){
        return Result.ok(service.listPage(params));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody LightingPlan plan){
        service.add(plan);
        return Result.ok();
    }

    @PostMapping("/edit")
    public Result<String> edit(@RequestBody LightingPlan plan){
        service.edit(plan);
        return Result.ok();
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long id){
        service.delete(id);
        return Result.ok();
    }

    @PostMapping("/enable")
    public Result<String> enable(@RequestBody LightingPlanExecutionTime data){
        service.enable(data);
        return Result.ok();
    }

    @PostMapping("/disable")
    public Result<String> disable(@RequestParam Long id){
        service.disable(id);
        return Result.ok();
    }

    @GetMapping("/detail")
    public Result<LightingPlanDetailDto> detail(@RequestParam Long id){
        return Result.ok(service.getDetail(id));
    }

    @PostMapping("/executeNow")
    public Result<String> executeNow(@RequestParam Long id){
        service.executionNow(id);
        return Result.ok();
    }
}
