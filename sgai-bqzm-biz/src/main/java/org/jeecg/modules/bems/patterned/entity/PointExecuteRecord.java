package org.jeecg.modules.bems.patterned.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@TableName("log_point_execute_record")
@Data
public class PointExecuteRecord {
    /** 主键. */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**策略执行纪录主键*/
    private Long strategyExecuteId;
    /**
     * 点位id
     */
    private Long pointId;
    /** 执行时间. */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedTime;
    /**
     * 设备id
     */
    private Long deviceId;
    /** 设备名称. */
    private String deviceName;
    /** 条件值. */
    private String conditionValue;
    /** 点位名称 */
    private String pointName;
    /** 是否执行成功 */
    private String successFlag;

    /**
     * 执行条件备注
     */
    private String conditionRemark;
}
