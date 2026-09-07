-- =====================================================================
-- 移除 lighting_circuit 上单规则报警的冗余字段
-- 说明：引入独立命中表 lighting_circuit_alarm 后，回路表上的
--       alarm_rule_code/alarm_rule_name/alarm_time/alarm_detail 不再需要
--       (它们只能表达单规则命中，且与命中表重复)。
--       alarm_flag 保留(原有列，语义改为"当前是否有任意命中"，由 job 同步/查询时用命中表推导)。
-- 数据库：达梦 DM8 / schema=BQZM
-- =====================================================================
ALTER TABLE BQZM.lighting_circuit DROP COLUMN alarm_rule_code;
ALTER TABLE BQZM.lighting_circuit DROP COLUMN alarm_rule_name;
ALTER TABLE BQZM.lighting_circuit DROP COLUMN alarm_time;
ALTER TABLE BQZM.lighting_circuit DROP COLUMN alarm_detail;
