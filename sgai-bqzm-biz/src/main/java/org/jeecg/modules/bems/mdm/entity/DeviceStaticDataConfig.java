package org.jeecg.modules.bems.mdm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;
import org.springframework.format.annotation.DateTimeFormat;

@EqualsAndHashCode(callSuper = true)
@TableName("device_static_data_config")
@Data
@ApiModel(value="deviceStaticDataConfig对象", description="设备静态数据配置")
public class DeviceStaticDataConfig extends BaseEntity {
    /**
     * 类型。基本信息：base；技术参数：tech；服务厂商：vendor
     */
    @ApiModelProperty(value = "类型。基本信息：base；技术参数：tech；服务厂商：vendor")
    private String type;
    /**
     * 标签
     */
    @ApiModelProperty(value = "标签")
    private String label;

    /**
     * 数据类型。文本输入框：input；下拉框：select；日期选择框：datePicker
     */
    @ApiModelProperty(value = "数据类型。文本输入框：input；下拉框：select；日期：datePicker")
    private String valueType;

    /**
     * 数据源
     */
    @ApiModelProperty(value = "数据源")
    private String valueData;

    /**
     * 排序字段。升序
     */
    @ApiModelProperty(value = "排序字段。升序")
    private Integer sort;
}
