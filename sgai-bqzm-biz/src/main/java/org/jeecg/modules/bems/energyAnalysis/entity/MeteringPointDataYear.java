package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("metering_point_data_year")
public class MeteringPointDataYear extends MeteringPointData {

    public static MeteringPointDataYear convert(MeteringPointData data){
        MeteringPointDataYear result = new MeteringPointDataYear();
        result.setMeteringPointId(data.getMeteringPointId());
        result.setTime(data.getTime());
        result.setValue(data.getValue());
        return result;
    }
}
