package org.jeecg.modules.bems.event.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 描述:事件
 *
 * @author ppliu-life
 * created in 2025/3/27 14:55
 */
@Data
public class Event {
    /** 主键. */
    private String id;
    /** 名称. */
    private String name;
    /** 工单编号. */
    private String code;
    /** 创建时间. */
    private LocalDateTime createdTime;
    /** 创建人 */
    private String creatPeopleId;
    /** 联系人. */
    private String contractPeople;
    /** 联系人电话. */
    private String contractPhone;
    /** 空间主键. */
    private String spaceId;
    /** 区域名称. */
    private String spaceName;
    /** 详细地址. */
    private String address;
    /** 事件分类. */
    private String orderType;
    /** 事件来源. */
    private String orderSource;
    /** 时限. */
    private Integer hours;
    /** 值班员. */
    private String attendant;
    /** 值班员主键. */
    private String attendantId;
    /** 描述. */
    private String description;
    /** 空间主键. */
    private String creatPeopleName;
    /** 附件 */
    private String pics;
    /** 状态. */
    private String status;
    /** 紧急程度. */
    private String urgency;
    /** 是否租区内. */
    private Boolean isArea;
    /** 是否有偿. */
    private Boolean isPaid;
    /** 价格. */
    private BigDecimal price;
    private String orderId;
    /**是否代客报事.*/
    private Boolean forCustomer;

}
