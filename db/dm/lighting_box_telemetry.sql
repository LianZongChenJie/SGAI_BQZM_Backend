-- =====================================================================
-- 箱子(电箱)遥测功能建表 SQL
-- 适用：达梦 DM / schema=BQZM（线上环境直接执行）
-- 包含：快照表、历史表、索引
-- 说明：本文件仅建表，不含任何测试数据；线上执行后由 MQ(DataType=7) 推送自动写入数据
-- =====================================================================

-- 1. 箱子遥测快照表（每个箱子一条最新记录，每次推送更新）
CREATE TABLE IF NOT EXISTS lighting_box_telemetry (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,   -- 主键(自增)
    gateway_code   VARCHAR(50),                        -- 箱子(网关)编号
    area_id        BIGINT,                             -- 所属区域id
    area_name      VARCHAR(100),                       -- 区域名称
    district_id    BIGINT,                             -- 所属片区id(片区名查询时从片区表反查,不冗余存储)
    voltage_a      DOUBLE,                             -- 交流电压1(A相, V)
    voltage_b      DOUBLE,                             -- 交流电压2(B相, V)
    voltage_c      DOUBLE,                             -- 交流电压3(C相, V)
    current_a      DOUBLE,                             -- 交流电流1(A相, A)
    current_b      DOUBLE,                             -- 交流电流2(B相, A)
    current_c      DOUBLE,                             -- 交流电流3(C相, A)
    active_power   DOUBLE,                             -- 有功功率(kW)
    reactive_power DOUBLE,                             -- 无功功率(kVar)
    apparent_power DOUBLE,                             -- 现在(视在)功率(kVA)
    power_factor   DOUBLE,                             -- 功率因数(0~1)
    total_energy   DOUBLE,                             -- 累积电量(kWh)
    collect_time   DATETIME,                           -- 采集时间
    update_time    DATETIME,                           -- 更新时间
    create_time    DATETIME,                           -- 创建时间
    sys_org_code   VARCHAR(64)                         -- 所属部门
);

COMMENT ON TABLE  lighting_box_telemetry IS '箱子(电箱)遥测最新快照表';
COMMENT ON COLUMN lighting_box_telemetry.gateway_code   IS '箱子(网关)编号';
COMMENT ON COLUMN lighting_box_telemetry.area_id        IS '所属区域id';
COMMENT ON COLUMN lighting_box_telemetry.area_name      IS '区域名称';
COMMENT ON COLUMN lighting_box_telemetry.district_id    IS '所属片区id(片区名查询时从片区表反查)';
COMMENT ON COLUMN lighting_box_telemetry.voltage_a      IS '交流电压1(A相,V)';
COMMENT ON COLUMN lighting_box_telemetry.voltage_b      IS '交流电压2(B相,V)';
COMMENT ON COLUMN lighting_box_telemetry.voltage_c      IS '交流电压3(C相,V)';
COMMENT ON COLUMN lighting_box_telemetry.current_a      IS '交流电流1(A相,A)';
COMMENT ON COLUMN lighting_box_telemetry.current_b      IS '交流电流2(B相,A)';
COMMENT ON COLUMN lighting_box_telemetry.current_c      IS '交流电流3(C相,A)';
COMMENT ON COLUMN lighting_box_telemetry.active_power   IS '有功功率(kW)';
COMMENT ON COLUMN lighting_box_telemetry.reactive_power IS '无功功率(kVar)';
COMMENT ON COLUMN lighting_box_telemetry.apparent_power IS '现在(视在)功率(kVA)';
COMMENT ON COLUMN lighting_box_telemetry.power_factor   IS '功率因数(0~1)';
COMMENT ON COLUMN lighting_box_telemetry.total_energy   IS '累积电量(kWh)';
COMMENT ON COLUMN lighting_box_telemetry.collect_time   IS '采集时间';

-- 2. 箱子遥测历史表（每次推送保存一条记录）
CREATE TABLE IF NOT EXISTS lighting_box_telemetry_history (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,   -- 主键(自增)
    gateway_code   VARCHAR(50),                        -- 箱子(网关)编号
    area_id        BIGINT,                             -- 所属区域id
    area_name      VARCHAR(100),                       -- 区域名称
    district_id    BIGINT,                             -- 所属片区id(片区名查询时从片区表反查,不冗余存储)
    voltage_a      DOUBLE,                             -- 交流电压1(A相, V)
    voltage_b      DOUBLE,                             -- 交流电压2(B相, V)
    voltage_c      DOUBLE,                             -- 交流电压3(C相, V)
    current_a      DOUBLE,                             -- 交流电流1(A相, A)
    current_b      DOUBLE,                             -- 交流电流2(B相, A)
    current_c      DOUBLE,                             -- 交流电流3(C相, A)
    active_power   DOUBLE,                             -- 有功功率(kW)
    reactive_power DOUBLE,                             -- 无功功率(kVar)
    apparent_power DOUBLE,                             -- 现在(视在)功率(kVA)
    power_factor   DOUBLE,                             -- 功率因数(0~1)
    total_energy   DOUBLE,                             -- 累积电量(kWh)
    collect_time   DATETIME,                           -- 采集时间
    create_time    DATETIME,                           -- 创建时间
    sys_org_code   VARCHAR(64)                         -- 所属部门
);

COMMENT ON TABLE  lighting_box_telemetry_history IS '箱子(电箱)遥测历史表';
COMMENT ON COLUMN lighting_box_telemetry_history.gateway_code   IS '箱子(网关)编号';
COMMENT ON COLUMN lighting_box_telemetry_history.area_id        IS '所属区域id';
COMMENT ON COLUMN lighting_box_telemetry_history.area_name      IS '区域名称';
COMMENT ON COLUMN lighting_box_telemetry_history.district_id    IS '所属片区id(片区名查询时从片区表反查)';
COMMENT ON COLUMN lighting_box_telemetry_history.voltage_a      IS '交流电压1(A相,V)';
COMMENT ON COLUMN lighting_box_telemetry_history.voltage_b      IS '交流电压2(B相,V)';
COMMENT ON COLUMN lighting_box_telemetry_history.voltage_c      IS '交流电压3(C相,V)';
COMMENT ON COLUMN lighting_box_telemetry_history.current_a      IS '交流电流1(A相,A)';
COMMENT ON COLUMN lighting_box_telemetry_history.current_b      IS '交流电流2(B相,A)';
COMMENT ON COLUMN lighting_box_telemetry_history.current_c      IS '交流电流3(C相,A)';
COMMENT ON COLUMN lighting_box_telemetry_history.active_power   IS '有功功率(kW)';
COMMENT ON COLUMN lighting_box_telemetry_history.reactive_power IS '无功功率(kVar)';
COMMENT ON COLUMN lighting_box_telemetry_history.apparent_power IS '现在(视在)功率(kVA)';
COMMENT ON COLUMN lighting_box_telemetry_history.power_factor   IS '功率因数(0~1)';
COMMENT ON COLUMN lighting_box_telemetry_history.total_energy   IS '累积电量(kWh)';
COMMENT ON COLUMN lighting_box_telemetry_history.collect_time   IS '采集时间';

-- 3. 历史表索引（按网关 + 时间查询历史数据）
-- 达梦不支持 CREATE INDEX IF NOT EXISTS，改用 PL/SQL 块判断存在后创建（幂等）
DECLARE
    idx_cnt INT;
BEGIN
    SELECT COUNT(*) INTO idx_cnt
      FROM USER_INDEXES
     WHERE INDEX_NAME = 'IDX_BOX_TELE_HISTORY_GW_TIME';
    IF idx_cnt = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX IDX_BOX_TELE_HISTORY_GW_TIME ON lighting_box_telemetry_history(gateway_code, collect_time)';
    END IF;
END;
/
