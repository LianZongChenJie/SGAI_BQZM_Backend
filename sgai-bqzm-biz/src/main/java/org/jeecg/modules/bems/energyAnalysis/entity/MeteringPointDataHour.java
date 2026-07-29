package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("metering_point_data_hour")
public class MeteringPointDataHour extends MeteringPointData{

    public static MeteringPointDataHour convert(MeteringPointData data){
        MeteringPointDataHour result = new MeteringPointDataHour();
        result.setMeteringPointId(data.getMeteringPointId());
        result.setTime(data.getTime());
        result.setValue(data.getValue());
        return result;
    }

}
