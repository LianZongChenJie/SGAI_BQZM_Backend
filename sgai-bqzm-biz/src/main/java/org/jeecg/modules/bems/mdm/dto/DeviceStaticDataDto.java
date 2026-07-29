package org.jeecg.modules.bems.mdm.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.modules.bems.mdm.entity.DeviceStaticData;

import java.util.List;

@Data
@ApiModel(value="deviceStaticDataDto", description="设备静态数据保存")
public class DeviceStaticDataDto {

    @ApiModelProperty(value = "设备id")
    private Long deviceId;

    @ApiModelProperty(value = "设备静态数据")
    private List<DeviceStaticData> staticDataList;

}
