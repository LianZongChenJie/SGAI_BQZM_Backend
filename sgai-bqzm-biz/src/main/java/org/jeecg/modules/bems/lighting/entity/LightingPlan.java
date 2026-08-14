package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 照明计划
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("lighting_plan")
public class LightingPlan extends BaseEntity {

    public static final String REL_TYPE_AREA = "区域";

    public static final String REL_TYPE_CIRCUIT = "回路";

    public static final String REL_TYPE_SCENE = "场景";

    public static final String OPERATION_TYPE_OPEN = "开启";
    public static final String OPERATION_TYPE_CLOSE = "关闭";

    public static final String STATUS_ENABLE = "启用";
    public static final String STATUS_DISABLE = "禁用";

    /**
     * 主键（雪花ID 19位，超过 JS 安全整数上限，JSON 序列化为字符串避免前端精度丢失）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    public Long id;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 关联类型。区域、回路
     */
    private String relType;

    /**
     * 关联id，多个id以英文逗号分隔
     */
    private String relIds;

    /**
     * 执行时间 HH:mm:ss
     */
    private String executionTime;

    /**
     * 操作类型。开启、关闭
     */
    private String operationType;

    /**
     * 启用、禁用
     */
    private String status;

    /**
     * 排序字段，升序排列
     */
    private Long sort;

    /**
     * 计划类型：普通计划、节日计划、应急计划
     */
    private String planType;

    /**
     * 周期类型：每天、工作日、周末、自定义
     */
    private String cycleType;

    /**
     * 节假日配置（JSON格式）
     */
    private String holidayConfig;

    /**
     * 目标类型：区域、回路、场景
     */
    private String targetType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 关联的定时任务ID（schedule_job.id，雪花ID），为空表示由照明计划页面创建
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long scheduleJobId;

    /**
     * 计划执行信息
     */
    @TableField(exist = false)
    private LightingPlanExecutionTime executionInfo;

    /**
     * 标签ID（非表字段，场景列表出参展示用，照明计划本身无标签）
     */
    @TableField(exist = false)
    private String tagId;

    /**
     * 标签名称（非表字段，场景列表出参展示用，照明计划本身无标签）
     */
    @TableField(exist = false)
    private String tagName;

    /**
     * 泛光节目ID（非表字段。场景本身没有 groupId，groupId 只属于节目 lighting_program.group_id；
     * 此字段保留仅为兼容出参结构，恒为 null，需要节目ID请通过 programSceneIds 查节目表）
     */
    @TableField(exist = false)
    private String groupId;

    /**
     * 类别（非表字段，场景列表出参展示用：节目类型、普通类型等，照明计划本身无此字段）
     */
    @TableField(exist = false)
    private String category;

    /**
     * 节目ID集合（非表字段，场景列表出参展示用：逗号分隔的 lighting_program.id，照明计划本身无此字段）
     */
    @TableField(exist = false)
    private String programSceneIds;

    /**
     * 当前正在运行的节目名称列表（非表字段，场景列表出参展示用）
     * 查询泛光总控系统 get_group_run_state，只显示运行中/开启的节目名称
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "当前正在运行的节目名称列表")
    private List<String> programDetail;

    /**
     * 引用的节目名称列表（非表字段，场景列表出参展示用：取自引用的节目 lighting_program.program_name）
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "引用的节目名称列表")
    private List<String> programNames;


    public LocalTime getExecutionLocalTime(){
        if(StringUtils.isEmpty(executionTime)){
            return null;
        }
    	return LocalTime.parse(executionTime, DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

}
