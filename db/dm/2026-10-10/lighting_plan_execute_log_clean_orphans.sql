-- =====================================================================
-- 控制日历：清理"孤儿"执行日志（删除定时任务后由延迟 MQ 补插的无名失败记录）
-- 创建日期：2026-10-10
-- 目标库  ：**生产库**（jdbc:dm://10.168.56.103:5236  schema=BQZM）
-- 状态    ：**未执行** —— 本脚本只产出，由实施/用户在受控客户端执行
-- ---------------------------------------------------------------------
-- 背景（根因，代码已于 2026-10-10 修复）：
--   1) 每天 23:00 LightingPlanJob → LightingSendService.sendPlan() 发延迟 MQ，
--      并写一条"待消费"执行日志（含 plan_name / execution_time / send_time）；
--   2) 计划被删除时，原 LightingPlanServiceImpl.delete() 会**连带删除该计划的全部执行日志**
--      → 消息到点被消费时，找不到对应的"待消费"记录；
--   3) LightingPlanExecuteLogServiceImpl.markConsumed() 原先**无条件补插**一条结果记录，
--      且不写 planName / executionTime / sendTime
--      → 控制日历上渲染为「00:00:00 关灯【计划id】」（三个值全是兜底值），
--        计划已删除、控制日志也没留痕，甲方完全无法辨认这些红条是什么。
--   ✅ 已修：计划不存在时不再补插；补插时补全名称与执行时间；同批次已有结果行不再补插。
--   ✅ 本脚本清理"修复前遗留"的这类记录 —— 生产库实测共 15 条（8/28×2、9/18×4、9/20×7、9/23×2）。
-- ---------------------------------------------------------------------
-- 影响面：仅删 BQZM.lighting_plan_execute_log 中"计划已不存在 + 无名称 + 无发送时间 + 执行失败"的行；
--         不动 lighting_plan、不动控制日志 lighting_operation_log、不动配置日志 lighting_config_log。
-- 可重复执行：第二次执行影响 0 行。
-- ⚠️ 本脚本含 DELETE / INSERT，`tools/run_sql.bat -db=prod` 的只读拦截会拒绝执行，
--    请在受控客户端（Navicat 等）里手工执行。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 步骤 0：备份待删行（便于回滚）。表已存在时先手工改名或跳过本步。
-- ---------------------------------------------------------------------
CREATE TABLE BQZM.lighting_plan_execute_log_bak_20261010 AS
SELECT * FROM BQZM.lighting_plan_execute_log
WHERE id IN (29,30,104,105,107,110,118,119,121,122,123,125,126,130,131);

-- ---------------------------------------------------------------------
-- 步骤 1：核对将要删除的行（应为 15 行）
-- ---------------------------------------------------------------------
SELECT id, plan_id, plan_name, execute_date, execution_time, status, send_time, consume_time, remark
FROM BQZM.lighting_plan_execute_log
WHERE id IN (29,30,104,105,107,110,118,119,121,122,123,125,126,130,131)
ORDER BY execute_date, id;

-- ---------------------------------------------------------------------
-- 步骤 2：删除（显式 id 列表，最可控）
-- ---------------------------------------------------------------------
DELETE FROM BQZM.lighting_plan_execute_log
WHERE id IN (29,30,104,105,107,110,118,119,121,122,123,125,126,130,131);

-- ---------------------------------------------------------------------
-- 步骤 3：复核（leftover_cnt 应为 0）
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS leftover_cnt
FROM BQZM.lighting_plan_execute_log
WHERE plan_name IS NULL
  AND send_time IS NULL
  AND status = '执行失败'
  AND plan_id NOT IN (SELECT id FROM BQZM.lighting_plan);

-- 剩余全部执行日志（应为 3 行 10 月的正常记录）
SELECT id, plan_id, plan_name, execute_date, execution_time, status, send_time, consume_time
FROM BQZM.lighting_plan_execute_log
ORDER BY id;

-- ---------------------------------------------------------------------
-- 附：条件版删除（等价，适合其他库 / 以后复用；执行前务必先跑步骤 1 核对）
-- ---------------------------------------------------------------------
-- DELETE FROM BQZM.lighting_plan_execute_log
-- WHERE plan_name IS NULL
--   AND send_time IS NULL
--   AND status = '执行失败'
--   AND plan_id NOT IN (SELECT id FROM BQZM.lighting_plan);

-- ---------------------------------------------------------------------
-- 回滚（若判断误删）：用备份表恢复；确认无误后再删掉备份表
-- ---------------------------------------------------------------------
-- INSERT INTO BQZM.lighting_plan_execute_log SELECT * FROM BQZM.lighting_plan_execute_log_bak_20261010;
-- DROP TABLE BQZM.lighting_plan_execute_log_bak_20261010;
