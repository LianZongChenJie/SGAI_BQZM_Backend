-- =====================================================================
-- 第 3 步 / 共 4 步：预演（只建临时表 + 查询，不动业务表）
--
-- 执行：tools\run_sql.bat db\dm\2026-10-28\03_preview.sql -db=prod
-- 前提：第 1、2 步已成功
--
-- 执行后看三个数，再决定第 4 步：
--   2.1 coverage   no_log 越小越好；orphan_logs 是已删回路的孤儿日志，正常会有
--   2.2 warn       closed_but_unclosed_log / 回路总数 = 重建值偏小的比例
--                  开发库实测 647/865 = 75%
--   2.3 diff       逐条对比 old_sec / new_sec；**开发库实测新值总量只有旧值的 10.3%**
--                  → 第 4 步（回写）默认建议跳过，详见 04_duration_strategy.sql 头部
-- =====================================================================


-- ---------------------------------------------------------------------
-- 一、抽事件 + 去抖 + 配对（只建 tmp_ 表）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS "tmp_circuit_ev";
CREATE TABLE "tmp_circuit_ev" AS
SELECT "id", "rel_id" AS "circuit_id", "operation_time" AS "t",
       CASE WHEN "operation_type" LIKE '%开启%' THEN 1 ELSE 0 END AS "is_on",
       LAG(CASE WHEN "operation_type" LIKE '%开启%' THEN 1 ELSE 0 END)
           OVER (PARTITION BY "rel_id" ORDER BY "operation_time", "id") AS "prev_on"
  FROM "lighting_operation_log"
 WHERE "log_type" = '回路'
   AND "rel_type" = '回路'
   AND "rel_id" IS NOT NULL
   AND "operation_time" IS NOT NULL
   AND ("operation_type" LIKE '%开启%' OR "operation_type" LIKE '%关闭%');

DROP TABLE IF EXISTS "tmp_circuit_ev2";
CREATE TABLE "tmp_circuit_ev2" AS
SELECT "id", "circuit_id", "t", "is_on"
  FROM "tmp_circuit_ev"
 WHERE "prev_on" IS NULL OR "prev_on" <> "is_on";

DROP TABLE IF EXISTS "tmp_circuit_dur";
CREATE TABLE "tmp_circuit_dur" AS
SELECT "circuit_id",
       SUM(CASE WHEN "is_on" = 1 AND "next_on" = 0
                THEN TIMESTAMPDIFF(SECOND, "t", "next_t") ELSE 0 END) AS "seconds",
       SUM(CASE WHEN "is_on" = 1 AND ("next_on" IS NULL OR "next_on" = 1)
                THEN 1 ELSE 0 END)                                    AS "unclosed_open",
       COUNT(*)                                                       AS "ev_cnt",
       MIN("t")                                                       AS "first_ev",
       MAX("t")                                                       AS "last_ev"
  FROM (SELECT e.*,
               LEAD("t")     OVER (PARTITION BY "circuit_id" ORDER BY "t", "id") AS "next_t",
               LEAD("is_on") OVER (PARTITION BY "circuit_id" ORDER BY "t", "id") AS "next_on"
          FROM "tmp_circuit_ev2" e) y
 GROUP BY "circuit_id";


-- ---------------------------------------------------------------------
-- 二、预演结果
-- ---------------------------------------------------------------------
-- 2.1 覆盖率自检（log_circuits 含孤儿日志，大于 circuit_total 属正常）
SELECT (SELECT COUNT(*) FROM "lighting_circuit")                         AS "circuit_total",
       (SELECT COUNT(*) FROM "tmp_circuit_dur")                          AS "log_circuits",
       (SELECT COUNT(*) FROM "lighting_circuit" c
          LEFT JOIN "tmp_circuit_dur" d ON d."circuit_id" = c."id"
         WHERE d."circuit_id" IS NULL)                                   AS "no_log",
       (SELECT COUNT(*) FROM "tmp_circuit_dur" d
         WHERE NOT EXISTS (SELECT 1 FROM "lighting_circuit" c
                            WHERE c."id" = d."circuit_id"))              AS "orphan_logs";

-- 2.2 数据可信度预警：库里已关闭、但日志末次事件是"开启"的回路数
--     （这些回路重建值必然偏小，缺最后一次关闭 → 占比高说明设备回传不写日志）
SELECT COUNT(*) AS "closed_but_unclosed_log"
  FROM "tmp_circuit_dur" d, "lighting_circuit" c
 WHERE c."id" = d."circuit_id"
   AND d."unclosed_open" > 0
   AND c."status" = '关闭';

-- 2.3 差异最大的 50 条（old_sec=旧值 / new_sec=重建值 / unclosed=未闭合开启数）
SELECT c."id", c."circuit_name", c."status",
       c."all_duration"                            AS "old_sec",
       COALESCE(d."seconds", 0)                    AS "new_sec",
       ROUND(COALESCE(d."seconds", 0) / 3600.0, 2) AS "new_hours",
       d."unclosed_open"                           AS "unclosed",
       d."ev_cnt"                                  AS "ev_cnt",
       d."first_ev"                                AS "first_ev",
       d."last_ev"                                 AS "last_ev"
  FROM "lighting_circuit" c
  LEFT JOIN "tmp_circuit_dur" d ON d."circuit_id" = c."id"
 ORDER BY ABS(COALESCE(c."all_duration", 0) - COALESCE(d."seconds", 0)) DESC
 FETCH FIRST 50 ROWS ONLY;
