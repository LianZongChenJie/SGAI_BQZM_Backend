package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;

/**
 * 照明场景分页查询参数
 */
@Data
public class LightingSceneQueryDto {

    /**
     * 场景名称（模糊匹配）
     */
    private String sceneName;

    /**
     * 场景名称别名（兼容前端只换 URL：传 planName 等价于 sceneName，均会按名称过滤）
     */
    private String planName;

    /**
     * 场景类型：普通场景、节日场景、应急场景
     */
    private String sceneType;

    /**
     * 类别：节目类型、普通类型等（精确匹配）
     */
    private String category;

    /**
     * 状态：启用、禁用
     */
    private String status;

    /**
     * 标签ID（精确匹配）
     */
    private String tagId;

    /**
     * 标签名称（模糊匹配）
     */
    private String tagName;

    /**
     * 节目ID集合（模糊匹配，如传某个节目ID查包含它的场景）
     */
    private String programSceneIds;

    private Integer pageNo = 1;

    private Integer pageSize = 10;
}
