-- =====================================================================
-- 照明回路报警历史流水表 lighting_circuit_alarm_log
-- 由诊断定时任务 LightingDiagJob 写入：标记报警时新增一条"报警中"记录，
-- 恢复时把该回路最近一条"报警中"记录回填恢复时间并置为"已恢复"。
-- 一条记录 = 一次完整的报警生命周期（含开始/结束时间、现场电流/额定）。
-- 数据库：达梦 DM8 / schema=BQZM
-- 幂等：表不存在则创建
-- =====================================================================
CREATE TABLE IF NOT EXISTS lighting_circuit_alarm_log (
    id                       BIGINT IDENTITY(1,1) PRIMARY KEY,  -- 主键(自增)
    circuit_id               BIGINT,                             -- 回路ID
    circuit_code             VARCHAR(50),                        -- 回路编码
    circuit_name             VARCHAR(100),                       -- 回路名称
    area_id                  BIGINT,                             -- 区域ID
    area_name                VARCHAR(100),                       -- 区域名称
    space                    VARCHAR(20),                        -- 空间编码(如 903)
    rule_code                VARCHAR(20),                        -- 命中规则编码 R1/R2
    rule_name                VARCHAR(50),                        -- 命中规则名称
    status                   VARCHAR(10) DEFAULT '报警中',        -- 状态：报警中/已恢复
    alarm_time               TIMESTAMP,                          -- 报警开始时间
    recover_time             TIMESTAMP,                          -- 恢复时间(已恢复时有)
    electric_current         DECIMAL(12,2),                      -- 报警时实时电流(A)
    rated_electric_current   DECIMAL(12,2),                      -- 报警时额定电流(A)
    detail                   VARCHAR(500),                       -- 报警详情/判定依据
    create_time              TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 创建时间
);

COMMENT ON TABLE  lighting_circuit_alarm_log IS '照明回路报警历史流水表';
COMMENT ON COLUMN lighting_circuit_alarm_log.id IS '主键ID';
COMMENT ON COLUMN lighting_circuit_alarm_log.circuit_id IS '回路ID';
COMMENT ON COLUMN lighting_circuit_alarm_log.circuit_code IS '回路编码';
COMMENT ON COLUMN lighting_circuit_alarm_log.circuit_name IS '回路名称';
COMMENT ON COLUMN lighting_circuit_alarm_log.area_id IS '区域ID';
COMMENT ON COLUMN lighting_circuit_alarm_log.area_name IS '区域名称';
COMMENT ON COLUMN lighting_circuit_alarm_log.space IS '空间编码(如903)';
COMMENT ON COLUMN lighting_circuit_alarm_log.rule_code IS '命中规则编码R1/R2';
COMMENT ON COLUMN lighting_circuit_alarm_log.rule_name IS '命中规则名称';
COMMENT ON COLUMN lighting_circuit_alarm_log.status IS '状态：报警中/已恢复';
COMMENT ON COLUMN lighting_circuit_alarm_log.alarm_time IS '报警开始时间';
COMMENT ON COLUMN lighting_circuit_alarm_log.recover_time IS '恢复时间(已恢复时有)';
COMMENT ON COLUMN lighting_circuit_alarm_log.electric_current IS '报警时实时电流(A)';
COMMENT ON COLUMN lighting_circuit_alarm_log.rated_electric_current IS '报警时额定电流(A)';
COMMENT ON COLUMN lighting_circuit_alarm_log.detail IS '报警详情/判定依据';
COMMENT ON COLUMN lighting_circuit_alarm_log.create_time IS '创建时间';
