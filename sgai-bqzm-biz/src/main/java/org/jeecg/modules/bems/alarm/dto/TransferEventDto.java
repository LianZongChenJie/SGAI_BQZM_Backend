package org.jeecg.modules.bems.alarm.dto;

import lombok.Data;

@Data
public class TransferEventDto {
    /**
     * 记录id
     */
    private Long recordId;
    /**
     * 联系人
     */
    private String contractPeople;
    /**
     * 联系电话
     */
    private String contractPhone;

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
