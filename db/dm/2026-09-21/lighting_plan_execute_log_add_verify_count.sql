-- =====================================================================
-- 持续验证「多次验证」改造 - 字段变更（2026-09-21）
-- 背景：持续验证由"只验一次"改为"可配置总轮次（plan:verify:times）"，需要记录已执行到第几轮。
-- 说明：verify_status/verify_time 仍是调度状态机（job 分钟级扫描"待验证 且 verify_time<=now"）；
--       verify_result 只记摘要（补发明细在控制日志 lighting_operation_log 里）；
--       verify_count 记已执行轮次（含当前轮，0=尚未执行）。
-- 数据库：达梦 DM8 / schema=BQZM
-- =====================================================================
ALTER TABLE "BQZM"."lighting_plan_execute_log" ADD "verify_count" INT DEFAULT 0;
COMMENT ON COLUMN "BQZM"."lighting_plan_execute_log"."verify_count" IS '持续验证已执行轮次（含当前轮；0=尚未执行。总轮次取 business_config: plan:verify:times）';

-- 核对
SELECT column_name, data_type, data_default FROM all_tab_columns
WHERE owner = 'BQZM' AND table_name = 'lighting_plan_execute_log'
  AND column_name IN ('verify_status', 'verify_time', 'verify_result', 'verify_count');
