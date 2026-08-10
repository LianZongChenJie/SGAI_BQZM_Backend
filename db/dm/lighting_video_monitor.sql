-- =====================================================================
-- 照明视频监控表 lighting_video_monitor
-- 设备监控-实时视频流 存储视频名称/播放地址/所属区域等信息
-- 数据库：达梦 DM8
-- 表名与实体 LightingVideoMonitor.java 对应（@TableName("lighting_video_monitor")）
-- =====================================================================
CREATE TABLE lighting_video_monitor (
    id            BIGINT IDENTITY(1,1) NOT NULL,      -- 主键ID（自增）
    video_name    VARCHAR(255),                       -- 视频名称
    video_address VARCHAR(1000),                      -- 视频地址（URL）
    area_id       BIGINT,                             -- 区域ID
    area_name     VARCHAR(255),                       -- 区域名称
    status        VARCHAR(20),                        -- 状态：在线、离线
    sort          INT,                                -- 排序
    remark        VARCHAR(500),                       -- 备注
    create_by     VARCHAR(50),                        -- 创建人
    create_time   DATETIME,                           -- 创建时间
    update_by     VARCHAR(50),                        -- 更新人
    update_time   DATETIME,                           -- 更新时间
    sys_org_code  VARCHAR(64),                        -- 所属部门
    PRIMARY KEY (id)
);

-- 字段注释
COMMENT ON TABLE  lighting_video_monitor IS '照明视频监控表';
COMMENT ON COLUMN lighting_video_monitor.id            IS '主键ID';
COMMENT ON COLUMN lighting_video_monitor.video_name    IS '视频名称';
COMMENT ON COLUMN lighting_video_monitor.video_address IS '视频地址';
COMMENT ON COLUMN lighting_video_monitor.area_id       IS '区域ID';
COMMENT ON COLUMN lighting_video_monitor.area_name     IS '区域名称';
COMMENT ON COLUMN lighting_video_monitor.status        IS '状态：在线、离线';
COMMENT ON COLUMN lighting_video_monitor.sort          IS '排序';
COMMENT ON COLUMN lighting_video_monitor.remark        IS '备注';
COMMENT ON COLUMN lighting_video_monitor.create_by     IS '创建人';
COMMENT ON COLUMN lighting_video_monitor.create_time   IS '创建时间';
COMMENT ON COLUMN lighting_video_monitor.update_by     IS '更新人';
COMMENT ON COLUMN lighting_video_monitor.update_time   IS '更新时间';
COMMENT ON COLUMN lighting_video_monitor.sys_org_code  IS '所属部门';

-- 区域ID索引（设备监控页按区域筛选视频）
CREATE INDEX idx_video_monitor_area ON lighting_video_monitor (area_id);
