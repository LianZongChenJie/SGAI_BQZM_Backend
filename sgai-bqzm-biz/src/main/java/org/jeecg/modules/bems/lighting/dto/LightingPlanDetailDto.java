package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingScene;

import java.util.List;

/**
 * 照明计划详情
 */
@Data
public class LightingPlanDetailDto {

    // ========== 基本信息 ==========
    /**
     * 计划id
     */
    private Long id;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 关联类型：区域、回路
     */
    private String relType;

    /**
     * 操作类型：开启、关闭
     */
    private String operationType;

    /**
     * 状态：启用、禁用
     */
    private String status;

    /**
     * 是否持续验证：0-否 1-是（定时执行成功后延迟N分钟复查灯状态，未达成的补下发）
     */
    private Integer verifyAfterExecute;

    // ========== 执行时间配置 ==========
    /**
     * 执行时间 HH:mm:ss
     */
    private String executionTime;

    /**
     * 开始日期 yyyy-MM-dd
     */
    private String startDate;

    /**
     * 结束日期 yyyy-MM-dd
     */
    private String endDate;

    /**
     * 启用的星期，逗号分隔 "1,2,3,4,5"
     */
    private String enabledWeek;

    /**
     * 版本号
     */
    private String version;

    // ========== 关联信息 ==========
    /**
     * 关联的区域列表（relType=区域时有值）
     */
    private List<LightingArea> areaList;

    /**
     * 关联回路列表（relType=回路时有值）
     */
    private List<LightingCircuit> circuitList;

    /**
     * 关联场景列表（relType=场景时有值）
     */
    private List<LightingScene> sceneList;
}
