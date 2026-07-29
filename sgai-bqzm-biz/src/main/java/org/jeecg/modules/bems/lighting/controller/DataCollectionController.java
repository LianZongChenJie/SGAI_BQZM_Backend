package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.service.IDataCollectionService;
import org.jeecg.modules.bems.lighting.vo.DataCollectionStatisticsVo;
import org.jeecg.modules.bems.lighting.vo.DataTypeDistributionVo;
import org.jeecg.modules.bems.lighting.vo.RealtimeDataItemVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据汇集
 */
@Api(tags = "照明-数据汇集")
@RestController
@RequestMapping("/bems/lighting/dataCollection")
@AllArgsConstructor
public class DataCollectionController {

    private final IDataCollectionService dataCollectionService;

    /**
     * 数据汇集统计（折线图）
     */
    @ApiOperation("数据汇集统计")
    @GetMapping("/statistics")
    public Result<DataCollectionStatisticsVo> statistics() {
        return Result.ok(dataCollectionService.getCollectionStatistics());
    }

    /**
     * 数据类型分布
     */
    @ApiOperation("数据类型分布")
    @GetMapping("/typeDistribution")
    public Result<List<DataTypeDistributionVo>> typeDistribution() {
        return Result.ok(dataCollectionService.getDataTypeDistribution());
    }

    /**
     * 实时数据流
     */
    @ApiOperation("实时数据流")
    @GetMapping("/realtimeData")
    public Result<List<RealtimeDataItemVo>> realtimeData(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(dataCollectionService.getRealtimeDataList(limit));
    }
}
