package org.jeecg.modules.bems.lighting.controller;

import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bems/lighting/dataStatistic")
@AllArgsConstructor
public class LightingDataStatisticController {

    private final ILightingCircuitService circuitService;

    @GetMapping("/getRunStatusStatistic")
    public Result<?> getRunStatusStatistic(){
        List<LightingCircuit> list = circuitService.list();
        long onLine = list.stream().filter(item -> LightingCircuit.COMSTAT_ONLINE.equals(item.getComstat())).count();
        long offLine = list.size() - onLine;
        Map<String,Object> result = new HashMap<>();
        result.put("categoryName","照明回路");
        result.put("onlineNum",onLine);
        result.put("offlineNum",offLine);
        result.put("totalNum",(long)list.size());
        List<Object> data = new ArrayList<>();
        data.add(result);
        return Result.ok(data);
    }
}
