package org.jeecg.modules.bems.lighting.controller;

import org.jeecg.common.api.vo.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 照明
 */
@RestController
@RequestMapping("/bems/lighting")
public class LightingController {

    @PostMapping("/refresh")
    public Result<?> refresh(){
        return Result.ok();
    }

}
