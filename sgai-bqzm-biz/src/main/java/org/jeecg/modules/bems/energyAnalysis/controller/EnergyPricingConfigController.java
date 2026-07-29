package org.jeecg.modules.bems.energyAnalysis.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyPricingConfig;
import org.jeecg.modules.bems.energyAnalysis.service.IEnergyPricingConfigService;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 能源价格配置
 */
@RestController
@RequestMapping("/bems/energyPricingConfig")
@AllArgsConstructor
public class EnergyPricingConfigController{

    private final IEnergyPricingConfigService service;

    @RequiresPermissions("bems:energyPricingConfig:save")
    @PostMapping("/save")
    public Result<String> save(@RequestBody EnergyPricingConfig data){
        service.save(data);
        return Result.ok();
    }

    @RequiresPermissions("bems:energyPricingConfig:add")
    @PostMapping("/add")
    public Result<String> add(@RequestBody EnergyPricingConfig data){
        service.add(data);
        return Result.ok();
    }

    @RequiresPermissions("bems:energyPricingConfig:update")
    @PostMapping("/update")
    public Result<String> update(@RequestBody EnergyPricingConfig data){
        service.update(data);
        return Result.ok();
    }

    /**
     * 启用
     * @param id 配置id
     */
    @PostMapping("/enable")
    public Result<String> enable(@RequestParam("id") Long id){
        service.enable(id);
        return Result.ok();
    }

    /**
     * 禁用
     * @param id 配置id
     */
    @PostMapping("/disable")
    public Result<String> disable(@RequestParam("id") Long id){
        service.disable(id);
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<List<EnergyPricingConfig>> list(){
        return Result.ok(service.list());
    }

    @GetMapping("/listPage")
    public Result<Page<EnergyPricingConfig>> listPage(EnergyPricingConfig params){
        return Result.ok(service.listPage(params));
    }


    @GetMapping("/get")
    public Result<EnergyPricingConfig> get(@RequestParam("id") Long id){
        return Result.ok(service.getById(id));
    }

}
