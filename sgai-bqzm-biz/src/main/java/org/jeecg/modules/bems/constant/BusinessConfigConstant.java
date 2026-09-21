package org.jeecg.modules.bems.constant;

public class BusinessConfigConstant {

    /**
     * 照明地块数值
     * 格式  3/7
     */
    public static final String PREVIEW_STATISTICS_LIGHTING_PLOT_NUM = "preview:statistics:lightingPlotNum";
    /**
     * 综合预览-统计-覆盖率
     * 直接填写数值，格式：100
     */
    public static final String PREVIEW_STATISTICS_COVERAGE = "preview:statistics:coverage";

    /**
     * 综合预览-统计-待处理报警
     * 待处理报警的数量
     */
    public static final String PREVIEW_STATISTICS_PENDING_ALARM = "preview:statistics:pendingAlarm";

    /**
     * 综合预览-控制-全部地块对应的场景id
     */
    public static final String PREVIEW_CONTROL_ALL_AREA_SCENEID = "preview:control:allAreaSceneId";

    /**
     * 综合预览-控制-全部地块对应的场景id（非 bqzm 角色使用）
     */
    public static final String PREVIEW_CONTROL_ALL_AREA_SCENEID_OTHER = "preview:control:allAreaSceneId:other";


    /**
     * 照明地块数值（非 bqzm 角色使用）
     * 格式  3/7
     */
    public static final String PREVIEW_STATISTICS_LIGHTING_PLOT_NUM_OTHER = "preview:statistics:lightingPlotNum:other";
    /**
     * 综合预览-统计-覆盖率（非 bqzm 角色使用）
     * 直接填写数值，格式：100
     */
    public static final String PREVIEW_STATISTICS_COVERAGE_OTHER = "preview:statistics:coverage:other";

    /**
     * 综合预览-统计-待处理报警（非 bqzm 角色使用）
     * 待处理报警的数量
     */
    public static final String PREVIEW_STATISTICS_PENDING_ALARM_OTHER = "preview:statistics:pendingAlarm:other";

    /**
     * bqzm 角色编码：拥有该角色时读取当前配置，否则读取 ":other" 配置。
     * 支持配置多个角色编码，英文逗号分隔（如 "bqzm,admin"），命中任意一个即视为 bqzm 角色。
     */
    public static final String ROLE_BQZM = "role:bqzm";

    /**
     * 计划"持续验证"全局开关（原为计划级字段 verify_after_execute，2026-09-21 改为全局）。
     * 取值："开启" / "关闭"；**只认"开启"两字**，其它（含"关闭"、空、填错、读取异常）一律按关闭处理。
     */
    public static final String PLAN_VERIFY_ENABLED = "plan:verify:enabled";

    /**
     * 计划"持续验证"延迟分钟数：计划定时执行成功后，延迟 N 分钟复查灯状态并对未达成的补下发（所有计划共用）。
     * 直接填写数值，格式：3
     */
    public static final String PLAN_VERIFY_DELAY_MINUTES = "plan:verify:delay:minutes";


}
