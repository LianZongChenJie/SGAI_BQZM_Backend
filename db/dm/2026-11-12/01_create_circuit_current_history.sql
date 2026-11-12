-- =====================================================================
-- 回路电流整点采样历史表（2026-11-12 新增）
--
-- 背景：lighting_circuit.electric_current 是"覆盖式最新值"，回路电流没有历史，
--       一旦被下一条 MQ 电流报文覆盖就查不到过去。本表由 LightingCircuitCurrentSnapshotJob
--       每小时整点（cron 0 0 * * * ?）把 903 全部回路当时的电流快照落一行。
--
-- 执行：tools\run_sql.bat db\dm\2026-11-12\01_create_circuit_current_history.sql -db=17
--       生产环境等上线时再用 -db=prod 执行同一份脚本（只建表，不改存量数据）。
--
-- 量级估算：903 = 272 条回路 × 24 小时 ≈ 6,500 行/天 ≈ 240 万行/年。
-- 暂不建清理任务（用户 2026-11-12 明确"先不清理"），后续要清时按 collect_time 删即可。
-- =====================================================================

CREATE TABLE "lighting_circuit_current_history" (
    "id"                      BIGINT IDENTITY(1,1) NOT NULL,
    "circuit_id"              BIGINT,
    "circuit_name"            VARCHAR(100),
    "circuit_code"            VARCHAR(100),
    "area_id"                 BIGINT,
    "area_code"               VARCHAR(50),
    "area_name"               VARCHAR(100),
    "district_id"             BIGINT,
    "space"                   VARCHAR(20),
    "status"                  VARCHAR(50),
    "electric_current"        DECIMAL(10,3),
    "rated_electric_current"  DECIMAL(10,3),
    "collect_time"            TIMESTAMP,
    "create_time"             TIMESTAMP,
    "sys_org_code"            VARCHAR(64),
    CLUSTER PRIMARY KEY ("id")
);

COMMENT ON TABLE "lighting_circuit_current_history" IS '回路电流整点采样历史（每小时一行）';
COMMENT ON COLUMN "lighting_circuit_current_history"."circuit_id" IS '回路ID（lighting_circuit.id）';
COMMENT ON COLUMN "lighting_circuit_current_history"."circuit_name" IS '回路名称（快照）';
COMMENT ON COLUMN "lighting_circuit_current_history"."circuit_code" IS '回路编号（快照）';
COMMENT ON COLUMN "lighting_circuit_current_history"."area_id" IS '区域ID';
COMMENT ON COLUMN "lighting_circuit_current_history"."district_id" IS '所属片区ID（数据权限用）';
COMMENT ON COLUMN "lighting_circuit_current_history"."space" IS '空间编码，当前仅 903';
COMMENT ON COLUMN "lighting_circuit_current_history"."status" IS '采样时的回路开关状态（开启/关闭）';
COMMENT ON COLUMN "lighting_circuit_current_history"."electric_current" IS '实时电流（A）';
COMMENT ON COLUMN "lighting_circuit_current_history"."rated_electric_current" IS '额定电流（A）';
COMMENT ON COLUMN "lighting_circuit_current_history"."collect_time" IS '采样时刻（整点，同一时刻只落一批）';

CREATE INDEX "idx_lcch_circuit_time" ON "lighting_circuit_current_history" ("circuit_id", "collect_time");

CREATE INDEX "idx_lcch_collect_time" ON "lighting_circuit_current_history" ("collect_time");

-- 核对：应为 1 / 15 列 / 2 个索引
SELECT COUNT(*) AS "tbl_created" FROM all_tables
 WHERE owner = 'BQZM' AND table_name = 'lighting_circuit_current_history';

SELECT COUNT(*) AS "col_cnt" FROM all_tab_columns
 WHERE owner = 'BQZM' AND table_name = 'lighting_circuit_current_history';

SELECT COUNT(*) AS "idx_cnt" FROM all_indexes
 WHERE owner = 'BQZM' AND table_name = 'lighting_circuit_current_history';
