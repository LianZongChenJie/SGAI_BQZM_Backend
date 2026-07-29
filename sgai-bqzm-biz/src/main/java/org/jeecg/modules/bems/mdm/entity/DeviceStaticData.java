package org.jeecg.modules.bems.mdm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

@EqualsAndHashCode(callSuper = true)
@TableName("device_static_data")
@Data
@ApiModel(value="device_static_data对象", description="设备静态数据")
public class DeviceStaticData extends BaseEntity {

    /**
     * 设备id
     */
    @ApiModelProperty(value = "设备id")
    private Long deviceId;

    @ApiModelProperty(value = "配置id")
    private Long configId;

    @ApiModelProperty(value = "值")
    private String value;

}
