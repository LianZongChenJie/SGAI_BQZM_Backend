-- =====================================================================
-- ⚠️⚠️⚠️ 这是**完整说明版，不要整份执行** ⚠️⚠️⚠️
--     本文件含 2 处 UPDATE + 1 处回写，一次跑完会跳过"看预演再回写"的人工确认。
--     实际执行请用同目录下的 4 个分步文件（run_sql 遇错会继续执行，必须拆开）：
--        01_precheck_and_backup.sql    前置检查 + 备份（不改数据）
--        02_fix_closing_time.sql       修脏：清 closing_time（幂等）
--        03_preview.sql                预演：建 tmp 表 + 对比（不改业务表）
--        04_duration_strategy.sql      【可选】历史时长处理，默认建议跳过
-- =====================================================================
--
-- 重建 lighting_circuit.all_duration（回路开启总时长，单位：秒）
-- 目标库：达梦 DM8（BQZM schema）
-- 执行：tools\run_sql.bat <本文件> -db=prod   （默认 -db=17 指开发库）
--
-- 背景：LightingCircuitServiceImpl 里"开启时清空 closing_time"被 MyBatis-Plus 的
--       NOT_NULL 更新策略吞掉 → closing_time 残留上一周期的值 → 关闭分支的幂等判断
--       (closing_time 非空则跳过) 永远不成立 → 关闭时间冻结、all_duration 停止累加。
--       表现为"关闭时间早于开启时间"且"所有回路时长都停在同一批的值上"。
--
-- ⚠️⚠️ 执行前必读（三条）
--   1) 本脚本**不在事务里**，按 〇 → 一 → 二 → 三 → 四 顺序分步执行；
--      请先只跑到"三、预演"，看过对比结果再执行"四、回写"。
--   2) 重建值只是"有操作日志以来"的**下界（会偏小）**：
--      开发库实测 865 条回路中 647 条(75%) 是"库里已关闭、但日志末次事件是开启"，
--      因为**设备 MQ 回传造成的开关不写 lighting_operation_log**（日志只记人为/计划下发）。
--      若无法接受偏小，请改用本文件末尾"方案 B：全部归零"。
--   3) 第二步会 UPDATE 清空 closing_time（不可通过本脚本回滚，所以备份排在最前）。
--
-- 数据源：lighting_operation_log（log_type='回路'，rel_type='回路'，
--         operation_type 为 '回路开启'/'回路关闭'），开发库实测 46350 条（2026-08-12 ~ 09-21）。
-- 算法：  1) 按 回路+时间 排序；
--         2) 连续重复的同类事件只保留第一条（去抖：区域全开/全关会给区域内每个回路
--            都写一条子日志，即使该回路实际状态没变，实测 closing_time 存在
--            "同一秒 20~36 个回路"的批量写入特征；去抖后 46350 → 30915 条）；
--         3) 按 "开启 -> 下一条关闭" 配对累加秒数；
--         4) 末尾未闭合的开启（还在亮着）不计入历史累计。
--
-- 方言核对（已在开发库 192.168.204.17 实测）：
--   DROP TABLE IF EXISTS / CREATE TABLE AS SELECT / LAG / LEAD / TIMESTAMPDIFF /
--   FETCH FIRST n ROWS ONLY 均可用；
--   达梦**不支持 UPDATE ... JOIN**，回写用"UPDATE 表别名 + 相关子查询 + EXISTS"，实测真正生效。
--
-- ⚠️ 开发库对象名是小写，必须加双引号；生产库连接串带 ignoreCase=true，加小写双引号同样匹配。
-- ⚠️ 本脚本只输出，不代执行；数据修复请在受控环境自行运行。
-- =====================================================================


-- ---------------------------------------------------------------------
-- 〇、前置检查（只读，先看清状态再动手）
-- ---------------------------------------------------------------------
-- 0.1 备份表是否已存在：若 >0 说明之前跑过，先决定用旧备份还是改名保留，
--     **不要直接 DROP 旧备份**（那可能是唯一的原始数据）
SELECT COUNT(*) AS "bak_exists"
  FROM all_tables
 WHERE owner = 'BQZM' AND table_name = 'bak_circuit_duration_20261028';

-- 0.2 当前脏数据规模（开发库实测：倒挂 6 / 开启态残留 22 = 全部开启态回路）
SELECT (SELECT COUNT(*) FROM "lighting_circuit"
         WHERE "closing_time" IS NOT NULL AND "start_time" IS NOT NULL
           AND "closing_time" < "start_time")                     AS "time_reversed",
       (SELECT COUNT(*) FROM "lighting_circuit"
         WHERE "status" = '开启' AND "closing_time" IS NOT NULL)   AS "on_with_closing",
       (SELECT COUNT(*) FROM "lighting_circuit")                   AS "circuit_total";


-- ---------------------------------------------------------------------
-- 一、备份（**必须在任何 UPDATE 之前执行，且只跑一次**）
--     故意不写 DROP：表已存在时这条会直接报错，起到"防止覆盖原始备份"的作用。
--     需要重跑：先手动改名旧备份，例如
--         ALTER TABLE "bak_circuit_duration_20261028" RENAME TO "bak_circuit_duration_20261028_a";
-- ---------------------------------------------------------------------
CREATE TABLE "bak_circuit_duration_20261028" AS
SELECT "id", "circuit_name", "status", "start_time", "closing_time", "all_duration",
       CURRENT_TIMESTAMP AS "bak_time"
  FROM "lighting_circuit";

-- 备份条数应等于 lighting_circuit 总数
SELECT COUNT(*) AS "bak_rows" FROM "bak_circuit_duration_20261028";


-- ---------------------------------------------------------------------
-- 二、修脏：清掉时间倒挂 / 开启态残留的 closing_time
--     （先备份再改；这两条是页面显示"关闭早于开启"的直接原因）
-- ---------------------------------------------------------------------
-- 2.1 开启态却挂着关闭时间（冻结行，开发库实测 22 条）
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


-- ---------------------------------------------------------------------
-- 三、@预演@ 生成重建结果（只建临时表，不动业务表）
-- ---------------------------------------------------------------------
-- 3.1 抽出回路级开关事件（一次查询内取"上一条状态"，用于去抖）
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

-- 3.2 连续同状态去抖（开发库实测 46350 → 30915）
DROP TABLE IF EXISTS "tmp_circuit_ev2";
CREATE TABLE "tmp_circuit_ev2" AS
SELECT "id", "circuit_id", "t", "is_on"
  FROM "tmp_circuit_ev"
 WHERE "prev_on" IS NULL OR "prev_on" <> "is_on";

-- 3.3 开启 -> 下一个关闭 配对求和
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
-- 四、@预演@ 对比与自检（只看结果，不动数据）
-- ---------------------------------------------------------------------
-- 4.1 差异最大的 50 条（old_sec=旧值 / new_sec=重建值 / no_log=该回路没有日志）
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

-- 4.2 覆盖率自检
--     ⚠️ "log_circuits" 会包含**已删回路的孤儿日志**（开发库 1115 个里 255 个是孤儿），
--        所以它大于 circuit_total 是正常的，看 orphan_logs 和 no_log 两个数。
SELECT (SELECT COUNT(*) FROM "lighting_circuit")                         AS "circuit_total",
       (SELECT COUNT(*) FROM "tmp_circuit_dur")                          AS "log_circuits",
       (SELECT COUNT(*) FROM "lighting_circuit" c
          LEFT JOIN "tmp_circuit_dur" d ON d."circuit_id" = c."id"
         WHERE d."circuit_id" IS NULL)                                   AS "no_log",
       (SELECT COUNT(*) FROM "tmp_circuit_dur" d
         WHERE NOT EXISTS (SELECT 1 FROM "lighting_circuit" c
                            WHERE c."id" = d."circuit_id"))              AS "orphan_logs";

-- 4.3 数据可信度预警：库里已关闭、但日志末次事件是"开启"的回路数
--     这些回路的重建值必然偏小（缺最后一次关闭），开发库实测 647/865。
--     若占比高，说明设备回传不写日志，建议改用"方案 B：全部归零"。
SELECT COUNT(*) AS "closed_but_unclosed_log"
  FROM "tmp_circuit_dur" d, "lighting_circuit" c
 WHERE c."id" = d."circuit_id"
   AND d."unclosed_open" > 0
   AND c."status" = '关闭';


-- ---------------------------------------------------------------------
-- 五、回写（**确认第四步结果合理后再执行**）
--     只更新"有日志"的回路；无日志的保持原值（新装/日志被清过的表不要被抹成 0）。
-- ---------------------------------------------------------------------
UPDATE "lighting_circuit" c
   SET c."all_duration" =
       (SELECT d."seconds" FROM "tmp_circuit_dur" d WHERE d."circuit_id" = c."id")
 WHERE EXISTS (SELECT 1 FROM "tmp_circuit_dur" d WHERE d."circuit_id" = c."id");

-- 5.2 无日志的回路可选归零（默认注释）：
-- UPDATE "lighting_circuit" c
--    SET c."all_duration" = 0
--  WHERE NOT EXISTS (SELECT 1 FROM "tmp_circuit_dur" d WHERE d."circuit_id" = c."id");


-- ---------------------------------------------------------------------
-- 六、校验
-- ---------------------------------------------------------------------
-- 6.1 负值（应为 0）
SELECT COUNT(*) AS "negative" FROM "lighting_circuit" WHERE "all_duration" < 0;

-- 6.2 时长分布（应不再出现"一个值覆盖全表"）
SELECT "all_duration", COUNT(*) AS "circuit_cnt"
  FROM "lighting_circuit"
 GROUP BY "all_duration"
 ORDER BY 2 DESC
 FETCH FIRST 10 ROWS ONLY;

-- 6.3 时间倒挂残留（应为 0）
SELECT COUNT(*) AS "reversed_left"
  FROM "lighting_circuit"
 WHERE "closing_time" IS NOT NULL AND "start_time" IS NOT NULL
   AND "closing_time" < "start_time";

-- 6.4 与备份对比：有没有被写成 NULL（应为 0）
SELECT COUNT(*) AS "null_after"
  FROM "lighting_circuit" c
  JOIN "bak_circuit_duration_20261028" b ON b."id" = c."id"
 WHERE c."all_duration" IS NULL AND b."all_duration" IS NOT NULL;


-- ---------------------------------------------------------------------
-- 七、清理临时表（核对完成后再执行）
-- ---------------------------------------------------------------------
-- DROP TABLE IF EXISTS "tmp_circuit_ev";
-- DROP TABLE IF EXISTS "tmp_circuit_ev2";
-- DROP TABLE IF EXISTS "tmp_circuit_dur";


-- =====================================================================
-- 方案 B：不接受"偏小"，全部归零、从修复上线后重新累计（二选一，别和第五步同时用）
--   优点：口径诚实（0 起点，之后每笔累加都准确）；缺点：历史累计彻底作废。
--   若选 B，把第五步的 UPDATE 注释掉，改用下面这条：
--
-- UPDATE "lighting_circuit" SET "all_duration" = 0;
-- =====================================================================


-- =====================================================================
-- 回滚（备份表在第一步，已含 closing_time / start_time，可完整还原）
-- UPDATE "lighting_circuit" c
--    SET c."all_duration" = (SELECT b."all_duration" FROM "bak_circuit_duration_20261028" b
--                             WHERE b."id" = c."id"),
--        c."start_time"   = (SELECT b."start_time" FROM "bak_circuit_duration_20261028" b
--                             WHERE b."id" = c."id"),
--        c."closing_time" = (SELECT b."closing_time" FROM "bak_circuit_duration_20261028" b
--                             WHERE b."id" = c."id")
--  WHERE EXISTS (SELECT 1 FROM "bak_circuit_duration_20261028" b WHERE b."id" = c."id");
--
-- 注意：备份不含 status，且回滚会把"关闭时间倒挂"的脏数据一起还原；
--       若只想回滚时长，把 start_time / closing_time 两行删掉即可。
-- =====================================================================


-- =====================================================================
-- 其他注意
-- 1) 重建值 = "有操作日志以来"的累计；日志被清理过的话会偏小，属预期（见文件头第 2 条）。
-- 2) 区域全开/全关、场景、计划执行都会给每个回路写子日志（状态没变也写），
--    脚本用"连续同状态去抖"抵消；极端乱序下仍可能有几秒误差。
-- 3) 代码修复（applyStatus → persistStatusFields）上线后，新周期会正常累加，不会再冻结。
-- 4) 本脚本与业务运行并发会互相影响：建议在低峰执行，且第二~五步连续跑完。
-- =====================================================================
