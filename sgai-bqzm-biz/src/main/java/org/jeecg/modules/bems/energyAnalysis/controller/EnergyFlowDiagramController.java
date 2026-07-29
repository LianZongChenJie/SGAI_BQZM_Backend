package org.jeecg.modules.bems.energyAnalysis.controller;

import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.energyAnalysis.dto.EnergyFlowDiagramDto;
import org.jeecg.modules.bems.energyAnalysis.service.IEnergyFlowDiagramService;
import org.jeecg.modules.bems.energyAnalysis.vo.EnergyFlowDiagramVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bems/energyFlowDiagram")
@AllArgsConstructor
public class EnergyFlowDiagramController {

    private final IEnergyFlowDiagramService service;

    @GetMapping("/findDay")
    @ApiOperation(value = "能流图-日查询")
    public Result<List<EnergyFlowDiagramVo>> findDay(EnergyFlowDiagramDto param) {
        return Result.ok(service.findDay(param.getType(), param.getDate()));
    }

    @GetMapping("/findMonth")
    @ApiOperation(value = "能流图-月查询")
    public Result<List<EnergyFlowDiagramVo>> findMonth(EnergyFlowDiagramDto param) {
        return Result.ok(service.findMonth(param.getType(), param.getDate()));
    }

    @GetMapping("/findYear")
    @ApiOperation(value = "能流图-年查询")
    public Result<List<EnergyFlowDiagramVo>> findYear(EnergyFlowDiagramDto param) {
        return Result.ok(service.findYear(param.getType(), param.getDate()));
    }


}
