-- =====================================================================
-- 第 1 步 / 共 4 步：前置检查 + 备份（**不改任何业务数据**，只建备份表）
--
-- 执行：tools\run_sql.bat db\dm\2026-10-28\01_precheck_and_backup.sql -db=prod
--
-- 为什么单独一步：run_sql 执行器遇到报错会**继续执行下一条**（tools/DmSqlRunner.java:239），
-- 所以备份不能和"修脏 UPDATE"放在同一个文件里 —— 万一备份失败，后面的 UPDATE 照样会跑。
--
-- 执行后核对：
--   1) bak_exists  必须为 0；若 >0 立刻停止，先处理旧备份（改名，不要 DROP）
--   2) bak_rows    必须等于 circuit_total
-- =====================================================================


-- ---------------------------------------------------------------------
-- 〇、前置检查（只读）
-- ---------------------------------------------------------------------
-- 0.1 备份表是否已存在
SELECT COUNT(*) AS "bak_exists"
  FROM all_tables
 WHERE owner = 'BQZM' AND table_name = 'bak_circuit_duration_20261028';

-- 0.2 当前脏数据规模（开发库实测：倒挂 6 / 开启态残留 22 / 总数 865）
SELECT (SELECT COUNT(*) FROM "lighting_circuit"
         WHERE "closing_time" IS NOT NULL AND "start_time" IS NOT NULL
           AND "closing_time" < "start_time")                     AS "time_reversed",
       (SELECT COUNT(*) FROM "lighting_circuit"
         WHERE "status" = '开启' AND "closing_time" IS NOT NULL)   AS "on_with_closing",
       (SELECT COUNT(*) FROM "lighting_circuit")                   AS "circuit_total";


-- ---------------------------------------------------------------------
-- 一、备份（故意不写 DROP：表已存在时这条会报错停下，防止覆盖唯一一份原始备份）
--     需要重跑：ALTER TABLE "bak_circuit_duration_20261028"
--                 RENAME TO "bak_circuit_duration_20261028_a";
-- ---------------------------------------------------------------------
CREATE TABLE "bak_circuit_duration_20261028" AS
SELECT "id", "circuit_name", "status", "start_time", "closing_time", "all_duration",
       CURRENT_TIMESTAMP AS "bak_time"
  FROM "lighting_circuit";

-- 1.2 备份条数应等于 0.2 的 circuit_total
SELECT COUNT(*) AS "bak_rows" FROM "bak_circuit_duration_20261028";
