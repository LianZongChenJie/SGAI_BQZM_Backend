package org.jeecg.modules.bems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据修改日志
 */
@Data
@TableName("data_amend_log")
@AllArgsConstructor
@NoArgsConstructor
public class DataAmendLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备id
     */
    private Long deviceId;

    /**
     * 小时数据id
     */
    private Long hourDataId;

    /**
     * 时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime time;

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

    /**
     * 修改前值
     */
    private BigDecimal originalValue;

    /**
     * 最终值
     */
    private BigDecimal value;
    /**
     * 操作人
     */
    private java.lang.String updateBy;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date updateTime;
}
