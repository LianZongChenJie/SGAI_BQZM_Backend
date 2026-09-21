-- =====================================================================
-- 持续验证「多次验证」改造 - 新增业务配置项（2026-09-21）
-- key = plan:verify:times   value = 总轮次（含首次），例：1 / 3 / 5
--   1 = 只验一次（与改造前行为一致，默认值）
--   >1 = 每轮复查并对未到位的补下发；轮次之间间隔沿用 plan:verify:delay:minutes；
--        任一轮"全部到位"即提前结束（不再无谓下发）；轮次用尽后置"已完成"
-- 说明：business_config.id 为自增列，插入时不指定 id
-- 幂等：已存在同 key 不重复插入；config_key 上有唯一约束
-- 数据库：达梦 DM8 / schema=BQZM
-- =====================================================================
INSERT INTO BQZM.business_config (name, config_key, config_value, remark)
SELECT '计划持续验证-验证轮次',
       'plan:verify:times',
       '1',
       '总轮次（含首次）：1=只验一次；N>1 时每轮间隔取 plan:verify:delay:minutes，任一轮全部到位则提前结束'
WHERE NOT EXISTS (SELECT 1 FROM BQZM.business_config WHERE config_key = 'plan:verify:times');

-- 核对（应与 plan:verify:delay:minutes 一起可见）
SELECT id, name, config_key, config_value FROM BQZM.business_config
WHERE config_key IN ('plan:verify:enabled', 'plan:verify:delay:minutes', 'plan:verify:times')
ORDER BY id;

-- ---------------------------------------------------------------------
-- 日常调整（按需单独执行，改完立即生效）
-- 改成 3 轮：UPDATE BQZM.business_config SET config_value = '3' WHERE config_key = 'plan:verify:times';
-- 改回单次：UPDATE BQZM.business_config SET config_value = '1' WHERE config_key = 'plan:verify:times';
-- ---------------------------------------------------------------------
