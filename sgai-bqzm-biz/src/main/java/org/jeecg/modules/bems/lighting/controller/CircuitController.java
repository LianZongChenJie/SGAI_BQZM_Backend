package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingCircuitQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 泛光照明-回路
 */
@RestController
@RequestMapping("/bems/lighting/circuit")
@AllArgsConstructor
public class CircuitController {

    private final ILightingCircuitService service;

    private final ILightingAreaService areaService;

    @GetMapping("/listPage")
    public Result<IPage<LightingCircuit>> listPage(LightingCircuitQueryDto param){
        return Result.ok(service.listPage(param));
    }

    @GetMapping("/all")
    public Result<List<LightingCircuit>> all(){
        List<LightingCircuit> circuits = service.list();
        Map<Long,LightingArea> areaMap = areaService.list()
                .stream().collect(Collectors.toMap(LightingArea::getId, Function.identity()));
        for (LightingCircuit circuit : circuits) {
            LightingArea area = areaMap.get(circuit.getAreaId());
            if(area != null){
                circuit.setAreaName(area.getAreaName());
                circuit.setSpaceName(area.getSpaceName());
            }
        }
        return Result.ok(circuits);
    }

    /**
     * 开启回路
     * @param id 回路id
     */
    @PostMapping("/open")
    public Result<String> open(@RequestParam Long id){
        service.open(id);
        return Result.ok();
    }

    /**
     * 关闭回路
     * @param id 回路id
     */
    @PostMapping("/close")
    public Result<String> close(@RequestParam Long id){
        service.close(id);
        return Result.ok();
    }

}
