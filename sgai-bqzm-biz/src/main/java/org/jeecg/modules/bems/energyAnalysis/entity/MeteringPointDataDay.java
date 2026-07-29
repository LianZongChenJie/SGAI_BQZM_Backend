package org.jeecg.modules.bems.energyAnalysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("metering_point_data_day")
public class MeteringPointDataDay extends MeteringPointData{

    public static MeteringPointDataDay convert(MeteringPointData data){
        MeteringPointDataDay hour = new MeteringPointDataDay();
        hour.setMeteringPointId(data.getMeteringPointId());
        hour.setTime(data.getTime());
        hour.setValue(data.getValue());
        return hour;
    }

}
