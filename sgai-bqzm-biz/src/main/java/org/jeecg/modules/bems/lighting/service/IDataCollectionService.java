package org.jeecg.modules.bems.lighting.service;

import org.jeecg.modules.bems.lighting.vo.DataCollectionStatisticsVo;
import org.jeecg.modules.bems.lighting.vo.DataTypeDistributionVo;
import org.jeecg.modules.bems.lighting.vo.RealtimeDataItemVo;

import java.util.List;

/**
 * 数据汇集服务
 */
public interface IDataCollectionService {

    /**
     * 数据汇集统计（折线图）
     */
    DataCollectionStatisticsVo getCollectionStatistics();

    /**
     * 数据类型分布
     */
    List<DataTypeDistributionVo> getDataTypeDistribution();

    /**
     * 实时数据流
     */
    List<RealtimeDataItemVo> getRealtimeDataList(int limit);
}
