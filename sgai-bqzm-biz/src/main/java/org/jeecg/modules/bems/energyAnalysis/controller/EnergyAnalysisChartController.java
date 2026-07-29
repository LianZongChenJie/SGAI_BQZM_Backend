package org.jeecg.modules.bems.energyAnalysis.controller;

import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyAnalysisChart;
import org.jeecg.modules.bems.energyAnalysis.service.IEnergyAnalysisChartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 能效分析-图表配置
 */
@RestController
@RequestMapping("/bems/energyAnalysis/chart")
@AllArgsConstructor
public class EnergyAnalysisChartController {

    private final IEnergyAnalysisChartService service;

    @RequiresPermissions("bems:energyAnalysis:chart:add")
    @PostMapping("/add")
    public Result<String> add(@RequestBody EnergyAnalysisChart data){
        service.add(data);
        return Result.ok();
    }

    @RequiresPermissions("bems:energyAnalysis:chart:update")
    @PostMapping("/update")
    public Result<String> update(@RequestBody EnergyAnalysisChart data){
        service.update(data);
        return Result.ok();
    }

    @RequiresPermissions("bems:energyAnalysis:chart:delete")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long id){
        service.delete(id);
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<List<EnergyAnalysisChart>> list(EnergyAnalysisChart params){
        return Result.ok(service.list(params));
    }

}
