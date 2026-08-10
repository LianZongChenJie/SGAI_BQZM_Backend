-- =====================================================================
-- 照明计划执行日志表 lighting_plan_execute_log
-- 记录照明计划 MQ 消息的"发送 → 消费"生命周期，用于日历展示执行成功/执行失败：
--   status = 待消费  （已发送延迟消息，尚未被消费）
--   status = 执行成功（MQ 消息已被消费，计划执行完成）
--   status = 执行失败（MQ 消息消费异常 / 计划执行失败）
-- 数据库：达梦 DM8
-- 表名与实体 LightingPlanExecuteLog.java 对应（@TableName("lighting_plan_execute_log")）
-- =====================================================================
CREATE TABLE lighting_plan_execute_log (
    id              BIGINT IDENTITY(1,1) NOT NULL,   -- 主键ID（自增）
    plan_id         BIGINT,                          -- 计划ID（lighting_plan.id）
    plan_name       VARCHAR(200),                    -- 计划名称
    version         VARCHAR(50),                     -- 计划版本号（执行时间配置版本）
    execute_date    VARCHAR(10),                     -- 计划执行日期 yyyy-MM-dd
    execution_time  VARCHAR(20),                     -- 计划执行时间 HH:mm:ss
    status          VARCHAR(20),                     -- 状态：待消费/执行成功/执行失败
    send_time       TIMESTAMP,                       -- MQ 消息发送时间
    consume_time    TIMESTAMP,                       -- MQ 消息消费时间
    remark          VARCHAR(500),                    -- 备注（失败原因等）
    create_by       VARCHAR(50),                     -- 创建人
    create_time     TIMESTAMP,                       -- 创建时间
    update_by       VARCHAR(50),                     -- 更新人
    update_time     TIMESTAMP,                       -- 更新时间
    sys_org_code    VARCHAR(50),                     -- 组织机构编码
    PRIMARY KEY (id)
);

-- 字段注释
COMMENT ON TABLE  lighting_plan_execute_log IS '照明计划执行日志（MQ 发送/消费追踪）';
COMMENT ON COLUMN lighting_plan_execute_log.id             IS '主键ID';
COMMENT ON COLUMN lighting_plan_execute_log.plan_id        IS '计划ID（lighting_plan.id）';
COMMENT ON COLUMN lighting_plan_execute_log.plan_name      IS '计划名称';
COMMENT ON COLUMN lighting_plan_execute_log.version        IS '计划版本号（执行时间配置版本）';
COMMENT ON COLUMN lighting_plan_execute_log.execute_date   IS '计划执行日期 yyyy-MM-dd';
COMMENT ON COLUMN lighting_plan_execute_log.execution_time IS '计划执行时间 HH:mm:ss';
COMMENT ON COLUMN lighting_plan_execute_log.status         IS '状态：待消费/执行成功/执行失败';
COMMENT ON COLUMN lighting_plan_execute_log.send_time      IS 'MQ 消息发送时间';
COMMENT ON COLUMN lighting_plan_execute_log.consume_time   IS 'MQ 消息消费时间';
COMMENT ON COLUMN lighting_plan_execute_log.remark         IS '备注（失败原因等）';
COMMENT ON COLUMN lighting_plan_execute_log.create_by      IS '创建人';
COMMENT ON COLUMN lighting_plan_execute_log.create_time    IS '创建时间';
COMMENT ON COLUMN lighting_plan_execute_log.update_by      IS '更新人';
COMMENT ON COLUMN lighting_plan_execute_log.update_time    IS '更新时间';
COMMENT ON COLUMN lighting_plan_execute_log.sys_org_code   IS '组织机构编码';
