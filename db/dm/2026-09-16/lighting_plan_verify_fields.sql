-- =====================================================================
-- 计划"持续验证"功能 - 字段变更（方案B：复用计划执行日志表）
-- 背景：计划定时执行后，因厂商问题 MQ 下发成功但实际没开/关灯。
--       勾选"持续验证"后，计划执行成功时登记一条待验证记录，
--       延迟 N 分钟（N 取自业务配置 business_config）后复查回路实际状态，
--       未达成的重新下发对应指令（只补发一次）。
-- 数据库：达梦 DM8 / schema=BQZM
-- =====================================================================

-- 1) lighting_plan 增加"持续验证"开关（0=否 1=是）
ALTER TABLE "BQZM"."lighting_plan" ADD "verify_after_execute" INT DEFAULT 0;
COMMENT ON COLUMN "BQZM"."lighting_plan"."verify_after_execute" IS '是否持续验证：0-否 1-是（计划执行后延迟N分钟复查灯状态，未达成的补下发）';

-- 2) lighting_plan_execute_log 增加验证相关字段
--    验证状态：无需(未开启持续验证)/待验证/已完成/已跳过
ALTER TABLE "BQZM"."lighting_plan_execute_log" ADD "verify_status" VARCHAR(10) DEFAULT '无需';
COMMENT ON COLUMN "BQZM"."lighting_plan_execute_log"."verify_status" IS '持续验证状态：无需/待验证/已完成/已跳过';

--    验证执行时刻（计划执行成功时刻 + 延迟分钟）
ALTER TABLE "BQZM"."lighting_plan_execute_log" ADD "verify_time" TIMESTAMP;
COMMENT ON COLUMN "BQZM"."lighting_plan_execute_log"."verify_time" IS '验证执行时刻(计划执行时刻+延迟分钟)';

--    验证/补发结果详情
ALTER TABLE "BQZM"."lighting_plan_execute_log" ADD "verify_result" VARCHAR(500);
COMMENT ON COLUMN "BQZM"."lighting_plan_execute_log"."verify_result" IS '验证/补发结果详情(补发的回路、跳过的目标等)';
