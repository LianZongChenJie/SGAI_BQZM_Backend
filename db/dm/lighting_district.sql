-- =====================================================================
-- 照明片区表 lighting_district
-- 片区（district）与区域（lighting_area）一对多：一个片区下包含多个区域
-- lighting_area 表新增 district_id 列关联本表 id
-- 数据库：达梦 DM8
-- 表名与实体 LightingDistrict.java 对应（@TableName("lighting_district")）
-- =====================================================================
CREATE TABLE lighting_district (
    id              BIGINT IDENTITY(1,1) NOT NULL,   -- 主键ID（自增）
    district_name   VARCHAR(200),                    -- 片区名称
    district_code   VARCHAR(200),                    -- 片区编码
    status          VARCHAR(50),                     -- 状态：启用、停用
    sort            INT,                             -- 排序
    space           VARCHAR(50),                     -- 空间编码
    space_name      VARCHAR(50),                     -- 空间名称
    location        VARCHAR(200),                    -- 位置信息
    remark          VARCHAR(200),                    -- 备注
    type            VARCHAR(50),                     -- 类型
    all_duration    BIGINT,                          -- 累计运行时长（秒）
    circuit_count   INT,                             -- 回路数
    online_count    INT,                             -- 在线数
    map_level       INT,                             -- 地图层级
    PRIMARY KEY (id)
);

-- 区域表增加所属片区ID
ALTER TABLE lighting_area ADD district_id BIGINT;

-- 字段注释
COMMENT ON TABLE  lighting_district IS '照明片区表';
COMMENT ON COLUMN lighting_district.id            IS '主键ID';
COMMENT ON COLUMN lighting_district.district_name IS '片区名称';
COMMENT ON COLUMN lighting_district.district_code IS '片区编码';
COMMENT ON COLUMN lighting_district.status        IS '状态：启用、停用';
COMMENT ON COLUMN lighting_district.sort          IS '排序';
COMMENT ON COLUMN lighting_district.space         IS '空间编码';
COMMENT ON COLUMN lighting_district.space_name    IS '空间名称';
COMMENT ON COLUMN lighting_district.location      IS '位置信息';
COMMENT ON COLUMN lighting_district.remark        IS '备注';
COMMENT ON COLUMN lighting_district.type          IS '类型';
COMMENT ON COLUMN lighting_district.all_duration  IS '累计运行时长（秒）';
COMMENT ON COLUMN lighting_district.circuit_count IS '回路数';
COMMENT ON COLUMN lighting_district.online_count  IS '在线数';
COMMENT ON COLUMN lighting_district.map_level     IS '地图层级';
COMMENT ON COLUMN lighting_area.district_id       IS '所属片区ID';
