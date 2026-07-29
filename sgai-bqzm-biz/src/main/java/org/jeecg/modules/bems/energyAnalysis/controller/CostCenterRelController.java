package org.jeecg.modules.bems.energyAnalysis.controller;

import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.energyAnalysis.dto.CostCenterRelDto;
import org.jeecg.modules.bems.energyAnalysis.service.ICostCenterRelService;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 成本中心关联关系
 */
@RestController
@RequestMapping("/bems/cost/center/rel")
@AllArgsConstructor
public class CostCenterRelController {

    private final ICostCenterRelService service;

    @RequiresPermissions("bems:cost:center:rel:saveRel")
    @PostMapping("/saveRel")
    public Result<String> saveRel(@RequestBody CostCenterRelDto data){
        service.saveRel(data.getCostCenterId(),data.getRelList());
        return Result.ok();
    }


    @GetMapping("/listByCostCenterId")
    public Result<List<MeteringPointVo>> listByCostCenterId(@RequestParam Long costCenterId){
        return Result.ok(service.listByCostCenterId(costCenterId)
                .stream()
                .map(MeteringPointVo::convert)
                .collect(Collectors.toList()));
    }

}
