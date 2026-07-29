package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("metering_point_data_month")
public class MeteringPointDataMonth extends MeteringPointData{

    public static MeteringPointDataMonth convert(MeteringPointData data){
        MeteringPointDataMonth result = new MeteringPointDataMonth();
        result.setMeteringPointId(data.getMeteringPointId());
        result.setTime(data.getTime());
        result.setValue(data.getValue());
        return result;
    }
}
