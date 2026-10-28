-- =====================================================================
-- 第 4 步 / 共 4 步：【可选】历史时长怎么处理 —— 默认建议**跳过本步**
--
-- 执行：tools\run_sql.bat db\dm\2026-10-28\04_duration_strategy.sql -db=prod
-- 前提：第 3 步预演已跑过
--
-- ⚠️ 实测对比（开发库 865 条回路，2026-10-28）：
--     旧 all_duration 总和 = 1,025,708,249 秒 ≈ 32.5 年（单条最大 1,440 万秒 ≈ 166 天）
--     日志重建总和       =   105,863,003 秒 ≈  3.35 年（单条最大 180 万秒 ≈ 21 天）
--     → 重建值只有旧值的 10.3%，因为**设备回传的开关不写操作日志**（日志只记人为/计划下发）。
--     → **方案 A（回写重建值）会把 90% 的历史累计砍掉，不推荐。**
--
-- 三种处理方式，按推荐度排序：
--   ① 跳过本步（推荐）：跑完第 1、2 步页面显示就正常了；代码上线后新周期照常累加。
--      旧值虽然冻结期间漏过累加，但不会继续变错，也不会被抹成 0。
--   ② 全部归零（口径干净）：历史作废，从修复上线那一刻从 0 重新累计 —— 取消注释 B 段。
--   ③ 回写重建值（不推荐）：把真实历史换成残缺日志值 —— 取消注释 A 段。
--
-- 无论选哪种，最后跑 C 段校验。
-- =====================================================================


-- ---------------------------------------------------------------------
-- A（不推荐）：回写"有日志以来"的重建值 —— 实测只有旧值的 10.3%
-- ---------------------------------------------------------------------
-- UPDATE "lighting_circuit" c
--    SET c."all_duration" =
--        (SELECT d."seconds" FROM "tmp_circuit_dur" d WHERE d."circuit_id" = c."id")
--  WHERE EXISTS (SELECT 1 FROM "tmp_circuit_dur" d WHERE d."circuit_id" = c."id");


-- ---------------------------------------------------------------------
-- B（可选）：全部归零，从修复上线后重新累计
-- ---------------------------------------------------------------------
-- UPDATE "lighting_circuit" SET "all_duration" = 0;


-- ---------------------------------------------------------------------
-- C 校验（不管选哪种都跑一遍）
-- ---------------------------------------------------------------------
-- C.1 负值（应为 0）
SELECT COUNT(*) AS "negative" FROM "lighting_circuit" WHERE "all_duration" < 0;

-- C.2 被写成 NULL 的条数（应为 0）
SELECT COUNT(*) AS "null_after"
  FROM "lighting_circuit" c
  JOIN "bak_circuit_duration_20261028" b ON b."id" = c."id"
 WHERE c."all_duration" IS NULL AND b."all_duration" IS NOT NULL;

-- C.3 时间倒挂残留（应为 0）
SELECT COUNT(*) AS "reversed_left"
  FROM "lighting_circuit"
 WHERE "closing_time" IS NOT NULL AND "start_time" IS NOT NULL
   AND "closing_time" < "start_time";

-- C.4 当前分布（跳过本步时，这里应与备份前的分布一致）
SELECT COUNT(*)          AS "circuits",
       SUM("all_duration") AS "sum_sec",
       MAX("all_duration") AS "max_sec"
  FROM "lighting_circuit";

-- C.5 与备份逐条对比（抽样差异最大的 20 条）
SELECT c."id", c."circuit_name",
       b."all_duration" AS "old_sec", c."all_duration" AS "new_sec"
  FROM "lighting_circuit" c
  JOIN "bak_circuit_duration_20261028" b ON b."id" = c."id"
 ORDER BY ABS(COALESCE(b."all_duration", 0) - COALESCE(c."all_duration", 0)) DESC
 FETCH FIRST 20 ROWS ONLY;


-- ---------------------------------------------------------------------
-- D 清理临时表（确认无误后再取消注释；建议先留着方便复查）
-- ---------------------------------------------------------------------
-- DROP TABLE IF EXISTS "tmp_circuit_ev";
-- DROP TABLE IF EXISTS "tmp_circuit_ev2";
-- DROP TABLE IF EXISTS "tmp_circuit_dur";


-- =====================================================================
-- 回滚（需要时用；备份表含 all_duration / start_time / closing_time）
-- UPDATE "lighting_circuit" c
--    SET c."all_duration" = (SELECT b."all_duration" FROM "bak_circuit_duration_20261028" b
--                             WHERE b."id" = c."id"),
--        c."start_time"   = (SELECT b."start_time" FROM "bak_circuit_duration_20261028" b
--                             WHERE b."id" = c."id"),
--        c."closing_time" = (SELECT b."closing_time" FROM "bak_circuit_duration_20261028" b
--                             WHERE b."id" = c."id")
--  WHERE EXISTS (SELECT 1 FROM "bak_circuit_duration_20261028" b WHERE b."id" = c."id");
-- =====================================================================
