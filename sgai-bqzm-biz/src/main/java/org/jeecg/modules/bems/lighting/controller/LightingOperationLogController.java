package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingOperationLogQueryDto;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 照明控制记录
 */
@RestController
@RequestMapping("/bems/lighting/operationLog")
@AllArgsConstructor
public class LightingOperationLogController {

    private final ILightingOperationLogService service;

    @GetMapping("/listPage")
    public Result<?> listPage(LightingOperationLogQueryDto param){
        return Result.ok(service.listPage(param));
    }
}
