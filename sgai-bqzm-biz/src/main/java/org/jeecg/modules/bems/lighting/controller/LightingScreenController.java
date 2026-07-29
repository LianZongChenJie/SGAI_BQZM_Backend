package org.jeecg.modules.bems.lighting.controller;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/bems/lighting/screen")
public class LightingScreenController {

    private final ILightingCircuitService circuitService;

    private final ILightingAreaService areaService;

    /**
     * 回路开关状态统计
     */
    @IgnoreAuth
    @GetMapping("/circuitStatusStatistics")
    public Result<?> circuitStatusStatistics() {
        List<LightingCircuit> list = circuitService.list();
        long onLine = list.stream().filter(item -> LightingCircuit.STATUS_ON.equals(item.getStatus())).count();
        long offLine = list.size() - onLine;
        Map<String, Object> result = new HashMap<>();
        result.put("onLine", onLine);
        result.put("offLine", offLine);
        result.put("total", list.size());
        return Result.ok(result);
    }

    /**
     * 回路开启时长排行
     *
     * @param quantity    数量
     * @param orderMethod 排序方式
     */
    @IgnoreAuth
    @GetMapping("/circuitOpenDurationRanking")
    public Result<?> circuitOpenDurationRanking(@RequestParam(required = false) Long quantity, @RequestParam(required = false) String orderMethod) {
        quantity = quantity == null ? 10 : quantity;
        orderMethod = orderMethod == null ? "desc" : orderMethod;
        Map<Long, String> areaMap = areaService.list()
                .stream()
                .collect(Collectors.toMap(LightingArea::getId, item -> item.getSpaceName() + item.getAreaName(), (k1, k2) -> k2));
        List<LightingCircuit> list = circuitService.list()
                .stream()
                .sorted(Comparator.comparing(LightingCircuit::getAllDuration).reversed())
                .limit(quantity)
                .peek(item -> {
                    item.setAllDuration(item.getAllDuration() / 60 / 60);
                    item.setCircuitName(areaMap.getOrDefault(item.getAreaId(), "其他区域") + item.getCircuitName());
                })
                .collect(Collectors.toList());
        return Result.ok(list);
    }

    /**
     * 建筑回路、区域回路开关状态
     *
     * @param type 0:全部、1:区域回路、2:建筑回路
     */
    @IgnoreAuth
    @GetMapping("/findAreaStatus")
    public Result<?> findAreaStatus(@RequestParam(required = false) String type) {
        List<LightingArea> list = areaService.list();
        if (StrUtil.isEmpty(type)) {
            return Result.ok(list);
        }
        return Result.ok(list.stream()
                .filter(item -> type.equals(item.getType()))
                .peek(item -> {
                    if (StrUtil.isEmpty(item.getStatus()))
                        item.setStatus("关闭");
                })
                .toList());
    }

}
