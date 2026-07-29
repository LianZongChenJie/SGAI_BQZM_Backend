package org.jeecg.modules.bems.event.dto;

import lombok.Data;

import java.util.List;

/**
 * 描述:
 *
 * @author ppliu-life
 * created in 2025/10/11 13:51
 */
@Data
public class EventSpace {
    /** 主键. */
    private Long id;
    /** 父级id. */
    private Long parentId;
    /** 名称. */
    private String name;
    /** 排序号 默认是0. */
    private Integer sortNum;
    /** 类型 有区域、建筑、层、房间 */
    private String type;
    /**全路径.*/
    private String fullName;
    private Boolean checked;
    private List<EventSpace> children;
}
