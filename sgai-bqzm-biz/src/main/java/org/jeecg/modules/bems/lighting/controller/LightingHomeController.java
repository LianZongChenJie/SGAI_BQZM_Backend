package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.permission.annotation.ButtonPermission;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.service.ILightingHomeService;
import org.jeecg.modules.bems.lighting.vo.AreaStatisticsVo;
import org.jeecg.modules.bems.lighting.vo.EnergyStatisticsVo;
import org.jeecg.modules.bems.lighting.vo.OnlineStatisticsVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 照明首页概览
 */
@Api(tags = "照明-首页概览")
@RestController
@RequestMapping("/bems/lighting/home")
@AllArgsConstructor
public class LightingHomeController {

    private final ILightingHomeService homeService;

    /**
     * 1. 地块数量和覆盖度统计
     */
    @ApiOperation("地块数量和覆盖度")
    @GetMapping("/areaStatistics")
    public Result<AreaStatisticsVo> areaStatistics() {
        return Result.ok(homeService.getAreaStatistics());
    }

    /**
     * 2. 在线设备和在线率统计
     */
    @ApiOperation("在线设备和在线率")
    @GetMapping("/onlineStatistics")
    public Result<OnlineStatisticsVo> onlineStatistics() {
        return Result.ok(homeService.getOnlineStatistics());
    }

    /**
     * 3. 今日用电和较昨日对比
     */
    @ApiOperation("今日用电和较昨日对比")
    @GetMapping("/energyStatistics")
    public Result<EnergyStatisticsVo> energyStatistics() {
        return Result.ok(homeService.getEnergyStatistics());
    }



    /**
     * 5. 地块运行状态
     * @param space 空间编码（可选，不传查全部）
     */
    @ApiOperation("地块运行状态")
    @GetMapping("/areaRunStatus")
    public Result<List<LightingArea>> areaRunStatus(@RequestParam(required = false) String space) {
        return Result.ok(homeService.getAreaRunStatus(space));
    }

    /**
     * 6. 一键控制所有灯
     * @param action open-全开、close-全关
     */
    @ApiOperation("一键控制所有灯")
    @ButtonPermission("northAreaLighting:switch")
    @PostMapping("/controlAll")
    public Result<String> controlAll(@RequestParam String action) {
        if ("open".equals(action)) {
            homeService.openAll();
        } else if ("close".equals(action)) {
            homeService.closeAll();
        } else {
            return Result.error("参数错误，action 只能是 open 或 close");
        }
        return Result.ok();
    }
}
