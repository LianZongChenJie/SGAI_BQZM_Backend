package org.jeecg.modules.bems.event.dto;

import lombok.Data;

@Data
public class BusinessDataDto {

    /**
     * 联系人
     */
    private String contractPeople;
    /**
     * 联系电话
     */
    private String contractPhone;
    /**
     * 是否客户
     */
    private Boolean forCustomer = false;
    /**
     * 是否租区
     */
    private Boolean isArea = true;
    /**
     * 是否有偿
     */
    private Boolean isPaid = false;
    /**
     * 事件类型
     */
    private String orderType = "设备告警";
    /**
     * 空间id
     */
    private String spaceId;
    /**
     * 空间全称
     */
    private String address;
    /**
     * 区域名称
     */
    private String spaceName;
    /**
     * 描述
     */
    private String description;

}
