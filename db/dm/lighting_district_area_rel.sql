-- =====================================================================
-- 照明片区-分组-区域关联表 lighting_district_area_rel
-- 片区（lighting_district）→ 分组（group_name）→ 区域（lighting_area）的关联关系：
--   一个片区下可建多个分组（分组名），一个分组下可挂多个区域；
--   同一片区同一分组内一个区域只能挂一次（唯一索引兜底）。
-- 数据库：达梦 DM8
-- 表名与实体 LightingDistrictAreaRel.java 对应（@TableName("lighting_district_area_rel")）
-- =====================================================================
CREATE TABLE lighting_district_area_rel (
    id            BIGINT IDENTITY(1,1) NOT NULL,   -- 主键ID（自增）
    district_id   BIGINT,                          -- 片区ID（关联 lighting_district.id）
    group_name    VARCHAR(100),                    -- 分组名称
    area_id       BIGINT,                          -- 区域ID（关联 lighting_area.id）
    area_name     VARCHAR(200),                    -- 区域名称（冗余，便于列表展示）
    sort          INT,                             -- 排序
    remark        VARCHAR(200),                    -- 备注
    create_by     VARCHAR(50),                     -- 创建人
    create_time   DATETIME,                        -- 创建时间
    update_by     VARCHAR(50),                     -- 更新人
    update_time   DATETIME,                        -- 更新时间
    sys_org_code  VARCHAR(64),                     -- 所属部门
    PRIMARY KEY (id)
);

-- 字段注释
COMMENT ON TABLE  lighting_district_area_rel IS '照明片区-分组-区域关联表';
COMMENT ON COLUMN lighting_district_area_rel.id           IS '主键ID';
COMMENT ON COLUMN lighting_district_area_rel.district_id  IS '片区ID（关联 lighting_district.id）';
COMMENT ON COLUMN lighting_district_area_rel.group_name   IS '分组名称';
COMMENT ON COLUMN lighting_district_area_rel.area_id      IS '区域ID（关联 lighting_area.id）';
COMMENT ON COLUMN lighting_district_area_rel.area_name    IS '区域名称';
COMMENT ON COLUMN lighting_district_area_rel.sort         IS '排序';
COMMENT ON COLUMN lighting_district_area_rel.remark       IS '备注';
COMMENT ON COLUMN lighting_district_area_rel.create_by    IS '创建人';
COMMENT ON COLUMN lighting_district_area_rel.create_time  IS '创建时间';
COMMENT ON COLUMN lighting_district_area_rel.update_by    IS '更新人';
COMMENT ON COLUMN lighting_district_area_rel.update_time  IS '更新时间';
COMMENT ON COLUMN lighting_district_area_rel.sys_org_code IS '所属部门';

-- 索引
-- 按片区查询分组下区域
CREATE INDEX idx_dar_district ON lighting_district_area_rel (district_id);
-- 按区域反查所属分组/片区
CREATE INDEX idx_dar_area ON lighting_district_area_rel (area_id);
-- 同一片区同一分组内一个区域只能挂一次
CREATE UNIQUE INDEX uk_dar_district_group_area ON lighting_district_area_rel (district_id, group_name, area_id);
