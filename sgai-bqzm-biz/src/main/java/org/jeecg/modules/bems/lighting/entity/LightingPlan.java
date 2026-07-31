package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 照明计划
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("lighting_plan")
public class LightingPlan extends BaseEntity {

    public static final String REL_TYPE_AREA = "区域";

    public static final String REL_TYPE_CIRCUIT = "回路";

    public static final String OPERATION_TYPE_OPEN = "开启";
    public static final String OPERATION_TYPE_CLOSE = "关闭";

    public static final String STATUS_ENABLE = "启用";
    public static final String STATUS_DISABLE = "禁用";

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
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
     * 关联的定时任务ID（schedule_job.id），为空表示由照明计划页面创建
     */
    private Long scheduleJobId;

    /**
     * 计划执行信息
     */
    @TableField(exist = false)
    private LightingPlanExecutionTime executionInfo;


    public LocalTime getExecutionLocalTime(){
        if(StringUtils.isEmpty(executionTime)){
            return null;
        }
    	return LocalTime.parse(executionTime, DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

}
