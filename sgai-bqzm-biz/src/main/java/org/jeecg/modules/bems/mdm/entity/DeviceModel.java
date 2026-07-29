package org.jeecg.modules.bems.mdm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

import java.io.Serializable;

/**
 * @Description 设备模型
 * @Author: jeecg-boot
 * @Date:   2025-04-15
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_model")
@ApiModel(value="设备模型", description="设备模型")
public class DeviceModel extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 设备模型名称
     */
    private String modelName;

    /**
     * 设备类别id
     */
    private Long categoryId;
}
