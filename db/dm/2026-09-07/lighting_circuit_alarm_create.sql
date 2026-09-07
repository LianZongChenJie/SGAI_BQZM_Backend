-- =====================================================================
-- 照明回路"当前命中规则"明细表 lighting_circuit_alarm
-- 一行 = 一个 回路 × 一条规则；同一回路可同时命中多条规则(含不同级别，
-- 如 R1=alarm 报警 与 R3=warn 预警 可并存)。由诊断定时任务 LightingDiagJob
-- 每轮对 903 空间全量重建当前命中(先删后插)，保证与实时数据一致。
-- 本表用于：当前报警/预警回路数量、各规则命中数、某回路命中规则列表。
-- 历史追溯走 lighting_circuit_alarm_log（一次报警生命周期）。
-- 数据库：达梦 DM8 / schema=BQZM
-- =====================================================================
CREATE TABLE IF NOT EXISTS lighting_circuit_alarm (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,  -- 主键(自增)
    circuit_id     BIGINT NOT NULL,                   -- 回路ID
    circuit_code   VARCHAR(50),                       -- 回路编码
    circuit_name   VARCHAR(100),                      -- 回路名称
    area_id        BIGINT,                            -- 区域ID(箱)
    area_name      VARCHAR(100),                      -- 区域名称
    space          VARCHAR(20),                       -- 空间编码(如 903)
    rule_code      VARCHAR(20) NOT NULL,              -- 命中规则编码 R1/R2/R3/R4...
    rule_name      VARCHAR(50),                       -- 命中规则名称
    rule_level     VARCHAR(10),                       -- 命中级别：alarm报警 / warn预警 / note提示
    alarm_time     TIMESTAMP,                         -- 命中时间
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  lighting_circuit_alarm IS '照明回路当前命中规则明细表';
COMMENT ON COLUMN lighting_circuit_alarm.id IS '主键ID';
COMMENT ON COLUMN lighting_circuit_alarm.circuit_id IS '回路ID';
COMMENT ON COLUMN lighting_circuit_alarm.circuit_code IS '回路编码';
COMMENT ON COLUMN lighting_circuit_alarm.circuit_name IS '回路名称';
COMMENT ON COLUMN lighting_circuit_alarm.area_id IS '区域ID(箱)';
COMMENT ON COLUMN lighting_circuit_alarm.area_name IS '区域名称';
COMMENT ON COLUMN lighting_circuit_alarm.space IS '空间编码(如903)';
COMMENT ON COLUMN lighting_circuit_alarm.rule_code IS '命中规则编码R1/R2/R3/R4';
COMMENT ON COLUMN lighting_circuit_alarm.rule_name IS '命中规则名称';
COMMENT ON COLUMN lighting_circuit_alarm.rule_level IS '命中级别：alarm报警/warn预警/note提示';
COMMENT ON COLUMN lighting_circuit_alarm.alarm_time IS '命中时间';
COMMENT ON COLUMN lighting_circuit_alarm.create_time IS '创建时间';
