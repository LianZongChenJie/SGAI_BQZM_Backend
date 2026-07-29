package org.jeecg.modules.bems.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("data_hour")
public class HourData extends MeterData{

    /**
     * 起始值
     */
    private BigDecimal startValue;

    /**
     * 结束值
     */
    private BigDecimal endValue;

    /**
     * 计算值
     */
    private BigDecimal computeValue;

    /**更新人*/
    @ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
    /**更新日期*/
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;
}
