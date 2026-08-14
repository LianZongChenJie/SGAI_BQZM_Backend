package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;

@Data
public class LightingAreaQueryDto {

    private Integer pageNo = 1;

    private Integer pageSize = 10;

    private String relName;

    private String areaName;

    private String space;

    /**
     * 空间名称（模糊查询）
     */
    private String spaceName;

    /**
     * 设备编号（F开头，模糊查询）
     */
    private String deviceNo;

    /**
     * 所属片区ID
     */
    private Long districtId;
}
