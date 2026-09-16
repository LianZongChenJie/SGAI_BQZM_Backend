-- =====================================================================
-- 新增业务配置项：计划"持续验证"延迟分钟数（所有计划共用，改后对新执行生效）
-- key = plan:verify:delay:minutes  value = 3（分钟）
-- 说明：business_config.id 为自增列，插入时不指定 id（由数据库自增生成）
-- 幂等：已存在同 key 则不重复插入
-- 数据库：达梦 DM8 / schema=BQZM
-- =====================================================================
INSERT INTO BQZM.business_config (name, config_key, config_value, remark)
SELECT '计划持续验证-延迟分钟',
       'plan:verify:delay:minutes',
       '3',
       '计划执行后延迟N分钟验证灯状态并补下发（单位分钟，所有计划共用）'
WHERE NOT EXISTS (SELECT 1 FROM BQZM.business_config WHERE config_key = 'plan:verify:delay:minutes');
