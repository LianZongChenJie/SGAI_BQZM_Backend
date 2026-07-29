package org.jeecg.modules.bems.mdm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.mdm.dto.DeviceAttributeHistoryQueryDto;
import org.jeecg.modules.bems.mdm.entity.DeviceAttributeHistory;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备属性历史值
 */
@RestController
@RequestMapping("/bems/deviceAttributeHistory")
@AllArgsConstructor
@Slf4j
public class DeviceAttributeHistoryController {

    private final IDeviceAttributeHistoryService service;

    @ApiOperation(value = "根据属性id查询", notes = "根据属性id查询")
    @GetMapping("/listByAttributeId")
    public Result<List<DeviceAttributeHistory>> listByAttributeId(DeviceAttributeHistoryQueryDto param) {
        return Result.ok(service.listByAttributeId(param));
    }
}
