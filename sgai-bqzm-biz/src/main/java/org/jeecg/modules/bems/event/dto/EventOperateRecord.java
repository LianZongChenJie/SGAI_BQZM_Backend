package org.jeecg.modules.bems.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 描述:工单操作记录
 *
 * @author maronglu
 * created in 2020/11/12
 */
@Data
public class EventOperateRecord {

    private String id;
    /**
     * 工单id
     */
    private String orderId;

    /**
     * 操作人id
     */
    private String operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operationTime;

    /**
     * 事件操作编码
     */
    private String operationCode;

    /**
     * 事件操作名称
     */
    private String operationName;
    /**
     * 事件操作别名
     */
    private String operationShowName;

    /**
     * 源状态名称
     */
    private String sourceStatusName;

    /**
     * 目标状态名称
     */
    private String targetStatusName;
    /**
     * 备注
     */
    private String remarks;

    /**
     * 流程类型
     */
    private String flowCode;
    private EventOrderAttributeJsonInfo eventOrderAttributeJsonInfo;

}
