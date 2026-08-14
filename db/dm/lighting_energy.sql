-- =====================================================================
-- 照明能耗表
-- 1) lighting_energy_read  分钟电量读数表
--    MQ 消息 DataType=6（电量）时，把每分钟的累计电量读数落库；
--    累计值（如 1 分钟前 100 度、1 分钟后 101 度），按 (gateway_code, circuit_code, read_time) 唯一。
-- 2) lighting_energy_hour  小时电量统计表
--    每小时整点由定时任务聚合上一小时（末条读数 - 上小时末条读数），供能耗统计接口使用。
-- 数据库：达梦 DM8
-- 表名与实体 LightingEnergyRead.java / LightingEnergyHour.java 对应
-- =====================================================================

-- 分钟电量读数表
CREATE TABLE lighting_energy_read (
    id           BIGINT IDENTITY(1,1) NOT NULL,   -- 主键ID（自增）
    gateway_code VARCHAR(50),                     -- 网关编号（GatewayCode，如 12）
    circuit_code VARCHAR(100),                    -- 回路编号（CircuitCode，为空表示网关级总表读数）
    area_id      BIGINT,                          -- 区域ID（解析到回路/区域时回填）
    area_code    VARCHAR(200),                    -- 区域编码（area_code）
    space        VARCHAR(50),                     -- 空间编码（space）
    value        DECIMAL(18,3),                   -- 累计电量（kWh）
    read_time    DATETIME,                        -- 读数时间
    create_time  DATETIME,                        -- 创建时间
    PRIMARY KEY (id)
);

-- 小时电量统计表
CREATE TABLE lighting_energy_hour (
    id           BIGINT IDENTITY(1,1) NOT NULL,   -- 主键ID（自增）
    stat_date    VARCHAR(10),                     -- 统计日期（yyyy-MM-dd）
    stat_hour    INT,                             -- 统计小时（0-23，该小时内的用电量）
    gateway_code VARCHAR(50),                     -- 网关编号
    circuit_code VARCHAR(100),                    -- 回路编号（为空表示网关级总表）
    area_id      BIGINT,                          -- 区域ID
    area_code    VARCHAR(200),                    -- 区域编码
    space        VARCHAR(50),                     -- 空间编码
    energy       DECIMAL(18,3),                   -- 该小时用电量（kWh）
    create_time  DATETIME,                        -- 创建时间
    PRIMARY KEY (id)
);

-- 字段注释
COMMENT ON TABLE  lighting_energy_read IS '照明分钟电量读数表';
COMMENT ON COLUMN lighting_energy_read.id           IS '主键ID';
COMMENT ON COLUMN lighting_energy_read.gateway_code IS '网关编号（GatewayCode）';
COMMENT ON COLUMN lighting_energy_read.circuit_code IS '回路编号（为空表示网关级总表）';
COMMENT ON COLUMN lighting_energy_read.area_id      IS '区域ID';
COMMENT ON COLUMN lighting_energy_read.area_code    IS '区域编码';
COMMENT ON COLUMN lighting_energy_read.space        IS '空间编码';
COMMENT ON COLUMN lighting_energy_read.value        IS '累计电量（kWh）';
COMMENT ON COLUMN lighting_energy_read.read_time    IS '读数时间';
COMMENT ON COLUMN lighting_energy_read.create_time  IS '创建时间';

COMMENT ON TABLE  lighting_energy_hour IS '照明小时电量统计表';
COMMENT ON COLUMN lighting_energy_hour.id           IS '主键ID';
COMMENT ON COLUMN lighting_energy_hour.stat_date    IS '统计日期（yyyy-MM-dd）';
COMMENT ON COLUMN lighting_energy_hour.stat_hour    IS '统计小时（0-23）';
COMMENT ON COLUMN lighting_energy_hour.gateway_code IS '网关编号';
COMMENT ON COLUMN lighting_energy_hour.circuit_code IS '回路编号（为空表示网关级总表）';
COMMENT ON COLUMN lighting_energy_hour.area_id      IS '区域ID';
COMMENT ON COLUMN lighting_energy_hour.area_code    IS '区域编码';
COMMENT ON COLUMN lighting_energy_hour.space        IS '空间编码';
COMMENT ON COLUMN lighting_energy_hour.energy       IS '该小时用电量（kWh）';
COMMENT ON COLUMN lighting_energy_hour.create_time  IS '创建时间';

-- 索引
-- 分钟表：同表同刻唯一（幂等），按时间窗口/表计查询
CREATE UNIQUE INDEX uk_energy_read_meter ON lighting_energy_read (gateway_code, circuit_code, read_time);
CREATE INDEX idx_energy_read_time ON lighting_energy_read (read_time);

-- 小时表：同日期同小时同表计唯一（整点任务幂等）
CREATE UNIQUE INDEX uk_energy_hour_meter ON lighting_energy_hour (stat_date, stat_hour, gateway_code, circuit_code);
CREATE INDEX idx_energy_hour_date ON lighting_energy_hour (stat_date, stat_hour);
