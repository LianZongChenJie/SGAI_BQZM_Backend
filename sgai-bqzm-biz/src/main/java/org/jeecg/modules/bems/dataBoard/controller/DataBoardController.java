package org.jeecg.modules.bems.dataBoard.controller;

import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.dataBoard.service.IDataBoardService;
import org.jeecg.modules.bems.dataBoard.vo.StatisticsVo;
import org.jeecg.modules.bems.energyAnalysis.vo.Chat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据看板
 */
@RestController
@RequestMapping("/bems/dataBoard")
@AllArgsConstructor
public class DataBoardController {

    private final IDataBoardService dataBoardService;

    /**
     * 能耗统计
     * @param dateType 日期类型，day,month,year
     */
    @GetMapping("/energyConsumptionStatistics")
    public Result<List<StatisticsVo>> energyConsumptionStatistics(@RequestParam String dateType) {
        // 调用服务层方法获取能耗统计数据
        List<StatisticsVo> statisticsVos = dataBoardService.getEnergyConsumptionStatistics(dateType);
        return Result.ok(statisticsVos);
    }

    /**
     * 近七日能耗趋势-电
     */
    @GetMapping("/energyConsumptionPSDElectricity")
    public Result<Chat> energyConsumptionPSDElectricity(){
        return Result.ok(dataBoardService.energyConsumptionPSDElectricity());
    }

    /**
     * 近七日能耗趋势-水
     */
    @GetMapping("/energyConsumptionPSDWater")
    public Result<Chat> energyConsumptionPSNWater(){
        return Result.ok(dataBoardService.energyConsumptionPSNWater());
    }
}
