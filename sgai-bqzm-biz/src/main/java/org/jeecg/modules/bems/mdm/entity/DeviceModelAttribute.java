package org.jeecg.modules.bems.mdm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

import java.io.Serializable;

/**
 * @Description 设备模型属性
 * @Author: jeecg-boot
 * @Date:   2025-04-15
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_model_attribute")
@ApiModel(value="设备模型属性", description="设备模型属性")
public class DeviceModelAttribute extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 模型id
     */
    private Long modelId;

    /**
     * 属性名称
     */
    private String attributeName;

    /**
     * 单位
     */
    private String unit;

    /**
     * 属性编码
     */
    private String attributeCode;

    /**
     * 读写等级。只读：1；读写：0
     */
    private String readwriteLevel;

    /**
     * 排序字段
     */
    private Integer sort;

    /**
     * 属性值类型
     */
    private String valueType;
    /**
     * 属性值配置
     */
    private String valueConfig;
}
