package org.jeecg.modules.bems.energyAnalysis.controller;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.energyAnalysis.constant.BusinessConfigConstant;
import org.jeecg.modules.bems.energyAnalysis.constant.MeteringPointConstant;
import org.jeecg.modules.bems.energyAnalysis.dto.CostAnalysisDto;
import org.jeecg.modules.bems.energyAnalysis.dto.MeteringPointDataDto;
import org.jeecg.modules.bems.energyAnalysis.service.ICostAnalysisService;
import org.jeecg.modules.bems.energyAnalysis.vo.Chat;
import org.jeecg.modules.bems.energyAnalysis.vo.CostVo;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointDataChartVo;
import org.jeecg.modules.bems.energyAnalysis.vo.chat.PieChat;
import org.jeecg.modules.bems.mdm.constant.DeviceConstant;
import org.jeecg.modules.bems.service.IBusinessConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成本分析
 */
@RestController
@RequestMapping("/bems/costAnalysis")
@AllArgsConstructor
public class CostAnalysisController {
    private final ICostAnalysisService service;

    private final IBusinessConfigService businessConfigService;

    @GetMapping("/findDay")
    public Result<MeteringPointDataChartVo> findDay(MeteringPointDataDto param) {
        return Result.ok(new MeteringPointDataChartVo(service.findDay(param.getEnergyFlowDiagramIds(), param.getDay())));
    }

    @GetMapping("/findMonth")
    public Result<MeteringPointDataChartVo> findMonth(MeteringPointDataDto param){
        return Result.ok(new MeteringPointDataChartVo(service.findMonth(param.getEnergyFlowDiagramIds(), param.getDay())));
    }

    @GetMapping("/findYear")
    public Result<MeteringPointDataChartVo> findYear(MeteringPointDataDto param){
        return Result.ok(new MeteringPointDataChartVo(service.findYear(param.getEnergyFlowDiagramIds(), param.getDay())));
    }
    /**
     * 获取总用量、总费用
     *
     * @param params
     *      category 类别。电：electricity；水：water；热：heating
     */
    @GetMapping("/getTotalCost")
    public Result<CostVo> getTotalCost(CostAnalysisDto params) {
        switch (params.getCategory()) {
            case "electricity":
                return Result.ok(service.getTotalCost(params.getDate(), getPointIds(BusinessConfigConstant.COST_TOTAL_ELECTRICITY_KEY)));
            case "water":
                return Result.ok(service.getTotalCost(params.getDate(), getPointIds(BusinessConfigConstant.COST_TOTAL_WATER_KEY)));
            case "heating":
                return Result.ok(service.getTotalCost(params.getDate(), getPointIds(BusinessConfigConstant.COST_TOTAL_HEAT_KEY)));
            default:
                return Result.error("类别错误");
        }
    }


    /**
     * 获取各专业分项成本饼状图
     * @param params
     * category 类别。电：electricity；水：water；热：heating
     */
    @GetMapping("/findSpecialtyPieChat")
    public Result<PieChat> findSpecialtyPieChat(CostAnalysisDto params) {
        switch (params.getCategory()) {
            case "electricity":
                return Result.ok(service.findSpecialtyPieChat(params.getDate(), getPointIds(BusinessConfigConstant.COST_CATEGORY_ELECTRICITY_KEY)));
            case "water":
                return Result.ok(service.findSpecialtyPieChat(params.getDate(), getPointIds(BusinessConfigConstant.COST_CATEGORY_WATER_KEY)));
            case "heating":
                return Result.ok(service.findSpecialtyPieChat(params.getDate(), getPointIds(BusinessConfigConstant.COST_TOTAL_HEAT_KEY)));
            default:
                return Result.error("类别错误");
        }
    }

    /**
     * 获取日成本折线图
     * @param params
     * category 类别。电：electricity；水：water；热：heating
     */
    @GetMapping("/findDayCost")
    public Result<Chat> findDayCost(CostAnalysisDto params){
        switch (params.getCategory()) {
            case "electricity":
                return Result.ok(service.findDayCost(params.getDate(), getPointIds(BusinessConfigConstant.COST_CATEGORY_ELECTRICITY_KEY)));
            case "water":
                return Result.ok(service.findDayCost(params.getDate(), getPointIds(BusinessConfigConstant.COST_CATEGORY_WATER_KEY)));
            case "heating":
                return Result.ok(service.findDayCost(params.getDate(), getPointIds(BusinessConfigConstant.COST_CATEGORY_HEAT_KEY)));
            default:
                return Result.error("类别错误");
        }
    }

    private List<Long> getPointIds(String configKey){
        String value = businessConfigService.getValueByKey(configKey);
        if(StringUtils.isEmpty(value)){
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(",")).map(Long::parseLong).collect(Collectors.toList());
    }

}
