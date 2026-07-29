package org.jeecg.modules.bems.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务配置项
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("business_config")
public class BusinessConfig extends BaseEntity{

    /**
     * 名称
     */
    private String name;
    /**
     * 编码
      */
    private String configKey;
    /**
     * 值
     */
    private String configValue;

    /**
     * 备注
     */
    private String remark;
}
