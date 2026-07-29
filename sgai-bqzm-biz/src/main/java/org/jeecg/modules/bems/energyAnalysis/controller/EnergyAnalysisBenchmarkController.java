package org.jeecg.modules.bems.energyAnalysis.controller;

import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyAnalysisBenchmark;
import org.jeecg.modules.bems.energyAnalysis.service.IEnergyAnalysisBenchmarkService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 能效分析-基准配置
 */
@RestController
@RequestMapping("/bems/energyAnalysis/benchmark")
@AllArgsConstructor
public class EnergyAnalysisBenchmarkController {
    private final IEnergyAnalysisBenchmarkService service;

    @RequiresPermissions("bems:energyAnalysisBenchmark:add")
    @PostMapping("/add")
    public Result<String> add(@RequestBody EnergyAnalysisBenchmark data){
        service.add(data);
        return Result.ok();
    }

    @RequiresPermissions("bems:energyAnalysisBenchmark:update")
    @PostMapping("/update")
    public Result<String> update(@RequestBody EnergyAnalysisBenchmark data){
        service.update(data);
        return Result.ok();
    }

    @RequiresPermissions("bems:energyAnalysisBenchmark:delete")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long id){
        service.delete(id);
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<List<EnergyAnalysisBenchmark>> list(EnergyAnalysisBenchmark params){
        return Result.ok(service.list(params));
    }

}
