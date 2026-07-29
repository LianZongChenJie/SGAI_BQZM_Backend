package org.jeecg.modules.bems.energyAnalysis.controller;

import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyAnalysisConfig;
import org.jeecg.modules.bems.energyAnalysis.service.IEnergyAnalysisConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 能效分析配置
 */
@RestController
@RequestMapping("/bems/energyAnalysis/config")
@AllArgsConstructor
public class EnergyAnalysisConfigController{
    private final IEnergyAnalysisConfigService service;

    /**
     * 添加
     */
    @RequiresPermissions("bems:energyAnalysisConfig:add")
    @PostMapping("/add")
    public Result<String> add(@RequestBody EnergyAnalysisConfig data){
        service.add(data);
        return Result.OK("添加成功！");
    }

    /**
     * 修改
     */
    @RequiresPermissions("bems:energyAnalysisConfig:update")
    @PostMapping("/update")
    public Result<String> update(@RequestBody EnergyAnalysisConfig data){
        service.update(data);
        return Result.OK("修改成功！");
    }

    /**
     * 启用
     */
    @RequiresPermissions("bems:energyAnalysisConfig:enable")
    @PostMapping("/enable")
    public Result<String> enable(@RequestParam Long id){
        service.enable(id);
        return Result.OK("启用成功！");
    }

    /**
     * 禁用
     * @param id 配置id
     */
    @RequiresPermissions("bems:energyAnalysisConfig:disable")
    @PostMapping("/disable")
    public Result<String> disable(@RequestParam Long id){
        service.disable(id);
        return Result.OK("禁用成功！");
    }

    @GetMapping("/list")
    public Result<List<EnergyAnalysisConfig>> list(EnergyAnalysisConfig params){
        return Result.ok(service.list(params));
    }

}
