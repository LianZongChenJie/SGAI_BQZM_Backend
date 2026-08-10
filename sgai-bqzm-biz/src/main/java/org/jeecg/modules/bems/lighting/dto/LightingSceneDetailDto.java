package org.jeecg.modules.bems.lighting.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;

import java.util.List;

/**
 * 照明场景详情
 * 出参结构与照明计划详情 plan/detail 一致（id/planName/relType/operationType/status/
 * executionTime/startDate/endDate/enabledWeek/version/areaList/circuitList + relName），
 * 便于前端只换 URL 即可复用。
 * 场景无定时配置，executionTime/startDate/endDate/enabledWeek/version 恒为 null。
 */
@Data
@ApiModel(value = "照明场景详情", description = "场景详情，出参结构同 plan/detail")
public class LightingSceneDetailDto {

    /**
     * 场景id（雪花ID，JSON 序列化为字符串避免前端精度丢失）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "场景ID")
    private Long id;

    /**
     * 场景名称（对应 plan/detail 的 planName）
     */
    @ApiModelProperty(value = "场景名称")
    private String planName;

    /**
     * 关联类型。区域、回路
     */
    @ApiModelProperty(value = "关联类型：区域、回路")
    private String relType;

    /**
     * 操作类型。开启、关闭
     */
    @ApiModelProperty(value = "操作类型：开启、关闭")
    private String operationType;

    /**
     * 状态：启用、禁用
     */
    @ApiModelProperty(value = "状态：启用、禁用")
    private String status;

    /**
     * 类别：节目类型、普通类型等（区分场景类别，如泛光节目/其他）
     */
    @ApiModelProperty(value = "类别：节目类型、普通类型等")
    private String category;

    /**
     * 泛光节目ID（场景按区域执行时若配置了节目ID则走泛光节目控制）
     */
    @ApiModelProperty(value = "泛光节目ID")
    private String groupId;

    /**
     * 执行时间 HH:mm:ss（场景无定时配置，恒为 null）
     */
    @ApiModelProperty(value = "执行时间（场景无定时配置，恒为 null）")
    private String executionTime;

    /**
     * 开始日期 yyyy-MM-dd（场景无定时配置，恒为 null）
     */
    @ApiModelProperty(value = "开始日期（场景无定时配置，恒为 null）")
    private String startDate;

    /**
     * 结束日期 yyyy-MM-dd（场景无定时配置，恒为 null）
     */
    @ApiModelProperty(value = "结束日期（场景无定时配置，恒为 null）")
    private String endDate;

    /**
     * 启用的星期，逗号分隔（场景无定时配置，恒为 null）
     */
    @ApiModelProperty(value = "启用的星期（场景无定时配置，恒为 null）")
    private String enabledWeek;

    /**
     * 版本号（场景无定时配置，恒为 null）
     */
    @ApiModelProperty(value = "版本号（场景无定时配置，恒为 null）")
    private String version;

    /**
     * 关联名称（取第一个目标的 relName，如"室外高杆路灯"）
     */
    @ApiModelProperty(value = "关联名称")
    private String relName;

    /**
     * 关联的区域列表（relType=区域时有值）
     */
    @ApiModelProperty(value = "关联的区域列表")
    private List<LightingArea> areaList;

    /**
     * 关联回路列表（relType=回路时有值）
     */
    @ApiModelProperty(value = "关联回路列表")
    private List<LightingCircuit> circuitList;

    /**
     * 标签ID（冗余存储，用于场景分组/筛选展示）
     */
    @ApiModelProperty(value = "标签ID")
    private String tagId;

    /**
     * 标签名称（冗余存储，用于场景分组/筛选展示）
     */
    @ApiModelProperty(value = "标签名称")
    private String tagName;

    /**
     * 节目类型场景ID集合（逗号分隔，关联 lighting_scene.id，即 category=节目 的场景）
     */
    @ApiModelProperty(value = "节目类型场景ID集合（逗号分隔，如 1,2,3）")
    private String programSceneIds;
}
