package org.jeecg.modules.bems.entity;

import lombok.Data;

@Data
public class SpaceInfo {

    /**
     * 空间id
     */
    private String id;
    /**
     * 名称
     */
    private String spaceName;

    /**
     * 全称
     */
    private String fullName;

}
