package org.jeecg.modules.bems.controller;

import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.entity.BusinessConfig;
import org.jeecg.modules.bems.service.IBusinessConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务配置
 */
@RestController
@RequestMapping("/bems/businessConfig")
@AllArgsConstructor
public class BusinessConfigController {

    private final IBusinessConfigService service;

    @PostMapping("/update")
    public Result<String> update(@RequestBody BusinessConfig config){
        service.updateByKey(config.getConfigKey(),config.getConfigValue());
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<List<BusinessConfig>> list(){
        return Result.ok(service.list());
    }
}
