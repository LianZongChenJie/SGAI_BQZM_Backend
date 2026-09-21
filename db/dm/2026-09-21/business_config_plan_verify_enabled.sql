-- =====================================================================
-- 持续验证：计划级字段 → 全局开关（2026-09-21）
-- key = plan:verify:enabled   value = 开启 / 关闭
--   开启：所有计划定时执行成功后，延迟 plan:verify:delay:minutes 分钟复查灯状态，未达成的补下发一次
--   关闭（含未配置、值填错、读取异常）：相当于不启用该功能，不登记验证、到期记录直接置"已跳过"
-- 背景：原为计划级字段 lighting_plan.verify_after_execute（前端勾选），现该字段保留但不再使用
-- 说明：business_config.id 为自增列，插入时不指定 id（由数据库自增生成）
-- 幂等：已存在同 key 不重复插入；config_key 上有唯一约束，重复插入会直接报错而非产生脏数据
-- 数据库：达梦 DM8 / schema=BQZM
-- =====================================================================
INSERT INTO BQZM.business_config (name, config_key, config_value, remark)
SELECT '计划持续验证开关',
       'plan:verify:enabled',
       '关闭',
       '开启：所有计划定时执行后延迟复查并补下发；关闭/填错：相当于不启用该功能'
WHERE NOT EXISTS (SELECT 1 FROM BQZM.business_config WHERE config_key = 'plan:verify:enabled');

-- 核对插入结果（应为 1 行，config_value = 关闭）
SELECT id, name, config_key, config_value, remark
FROM BQZM.business_config
WHERE config_key = 'plan:verify:enabled';

-- ---------------------------------------------------------------------
-- 日常开关（按需单独执行，改完立即生效，无需重启）
-- 开启：UPDATE BQZM.business_config SET config_value = '开启' WHERE config_key = 'plan:verify:enabled';
-- 关闭：UPDATE BQZM.business_config SET config_value = '关闭' WHERE config_key = 'plan:verify:enabled';
-- 也可直接用页面上改：业务配置 → 计划持续验证开关（仅黑色主题有该页面）
-- 注意：每次变更后端会写一条配置日志（修改系统配置 / 计划持续验证开关：旧 → 新）
-- ---------------------------------------------------------------------
