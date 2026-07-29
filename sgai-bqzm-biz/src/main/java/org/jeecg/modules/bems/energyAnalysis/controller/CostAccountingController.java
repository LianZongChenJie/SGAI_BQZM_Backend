package org.jeecg.modules.bems.energyAnalysis.controller;

import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.energyAnalysis.dto.CostAnalysisDto;
import org.jeecg.modules.bems.energyAnalysis.service.ICostAccountingService;
import org.jeecg.modules.bems.energyAnalysis.vo.CostAccountingVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 成本核算
 */
@RestController
@RequestMapping("/bems/cost/accounting")
@AllArgsConstructor
public class CostAccountingController {

    private final ICostAccountingService service;

    /**
     * 成本核算-年
     * @param params date，costCenterId
     */
    @GetMapping("/findCostByYear")
    public Result<List<CostAccountingVo>> findCostByYear(CostAnalysisDto params){
        return Result.ok(service.findCostByYear(params.getCostCenterId(),params.getDate()));
    }

    /**
     * 成本核算-月
     * @param params date，costCenterId
     */
    @GetMapping("/findCostByMonth")
    public Result<List<CostAccountingVo>> findCostByMonth(CostAnalysisDto params){
        return Result.ok(service.findCostByMonth(params.getCostCenterId(),params.getDate()));
    }

    /**
     * 成本核算-日
     * @param params date，costCenterId
     */
    @GetMapping("/findCostByDay")
    public Result<List<CostAccountingVo>> findCostByDay(CostAnalysisDto params){
        return Result.ok(service.findCostByDay(params.getCostCenterId(),params.getDate()));
    }
}
