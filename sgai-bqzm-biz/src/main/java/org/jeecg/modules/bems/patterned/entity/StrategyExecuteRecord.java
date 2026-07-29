package org.jeecg.modules.bems.patterned.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 策略执行记录
 */
@TableName("log_strategy_execute_record")
@Data
public class StrategyExecuteRecord {

    /**
     * 模式化管理
     */
    public static final String BusinessType_Patterning = "0" ;
    /**
     * 联动策略
     */
    public static final String BusinessType_Linkage = "1" ;

    /**
     * 成功
     */
    public static final String SuccessFlag_Success = "成功";

    /**
     * 失败
     */
    public static final String SuccessFlag_Fail = "失败";

    /**
     * 执行中
     */
    public static final String SuccessFlag_Executing = "执行中";

    /** 主键. */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 执行业务类型.【0，模式化管理，1，联动策略】 */
    private String businessType;
    /** 执行业务主键. */
    private Long businessKey;
    /** 是否执行成功 */
    private String successFlag;
    /** 描述. */
        private String description;
    /** 执行时间. */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedTime;
    /**点位执行纪录*/
    @TableField(exist = false)
    private List<PointExecuteRecord> pointExecuteRecordList;

    /**
     * 执行人
     */
    private String executedBy;
}
