package org.jeecg.modules.bems.lighting.service;

/**
 * 计划"持续验证"服务
 * <p>
 * 背景：计划定时执行后，厂商 MQ 下发成功但实际未开/关灯。开启"持续验证"的计划，
 * 在执行成功后延迟 N 分钟（N 取业务配置 {@code plan:verify:delay:minutes}）复查计划目标的
 * 灯实际状态，未达成的重新下发对应开关指令（只补发一次）。
 * <p>
 * 只针对"定时执行"（MQ 消费），手动"立即执行"不登记验证，避免污染控制日历的执行状态判定。
 */
public interface ILightingPlanVerifyService {

    /**
     * 定时执行成功后登记持续验证（仅当计划勾选了"持续验证"时生效）。
     * 会把该次执行日志的 verify_status 置为"待验证"并设置 verify_time。
     *
     * @param planId      计划ID
     * @param version     计划版本号
     * @param executeDate 执行日期 yyyy-MM-dd
     */
    void registerVerify(Long planId, String version, String executeDate);

    /**
     * 扫描到期的"待验证"记录并执行验证 + 按需补发（由定时任务每分钟调用）
     */
    void executePendingVerify();
}
