package org.jeecg.modules.bems.event.dto;

import lombok.Data;


/**
 * 描述:
 *
 * @author maronglu
 * created in 2020/12/8
 */
@Data
public class EventOrderAttributeJsonInfo {

    private String id;
    private String orderId;
    private String orderRecordId;
    private String content;
    private String flowCode;
    private String operationName;

}
