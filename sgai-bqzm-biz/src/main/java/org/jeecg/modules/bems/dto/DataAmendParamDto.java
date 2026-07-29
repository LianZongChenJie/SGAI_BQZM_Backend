package org.jeecg.modules.bems.dto;

import lombok.Data;

import java.util.List;

@Data
public class DataAmendParamDto {

    /**
     * 设备id
     */
    public Long deviceId;

    private String spaceIds;
    private List<Long> spaceIdList;

    private String deviceName;

    private String deviceCode;

    /**
     * 修正类型
     * 系统修正
     * 人工修正
     */
    private String amendType;

    /**
     * 分页参数
     */
    private Integer pageSize = 10;
    /**
     * 分页参数
     */
    private Integer pageNo = 1;

}
