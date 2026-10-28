-- =====================================================================
-- 第 2 步 / 共 4 步：修脏 —— 清掉时间倒挂 / 开启态残留的 closing_time
--
-- 执行：tools\run_sql.bat db\dm\2026-10-28\02_fix_closing_time.sql -db=prod
--
-- 前提：
--   1) 第 1 步已成功，备份表 bak_circuit_duration_20261028 已建好且条数对得上；
--   2) **修复代码已上线**（LightingCircuitServiceImpl.persistStatusFields）。
--      旧代码还在跑的话，这里清掉的 closing_time 会被旧逻辑重新写脏。
--
-- 本步幂等：重复执行第二次影响行数都是 0。
-- 执行后核对：2.3 的两个数都应为 0。
-- =====================================================================


-- 2.1 开启态却挂着关闭时间（就是本次 bug 的冻结行，开发库实测 22 条 = 全部开启态）
UPDATE "lighting_circuit"
   SET "closing_time" = NULL
 WHERE "status" = '开启'
   AND "closing_time" IS NOT NULL;

-- 2.2 关闭时间早于开启时间（开发库实测 6 条）
UPDATE "lighting_circuit"
   SET "closing_time" = NULL
 WHERE "closing_time" IS NOT NULL
   AND "start_time" IS NOT NULL
   AND "closing_time" < "start_time";

-- 2.3 复查（两个数都应为 0）
SELECT (SELECT COUNT(*) FROM "lighting_circuit"
         WHERE "closing_time" IS NOT NULL AND "start_time" IS NOT NULL
           AND "closing_time" < "start_time")                     AS "time_reversed_left",
       (SELECT COUNT(*) FROM "lighting_circuit"
         WHERE "status" = '开启' AND "closing_time" IS NOT NULL)   AS "on_with_closing_left";
