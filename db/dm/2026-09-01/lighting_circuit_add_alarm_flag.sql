-- ============================================================
-- lighting_circuit 表增加"是否报警"字段
-- 值：'报警' / '正常'，默认 '正常'
-- 说明：表里没有 alarm_flag 列时执行一次即可；已存在则跳过
-- 数据库：达梦 DM8
-- ============================================================

ALTER TABLE "BQZM"."lighting_circuit" ADD "alarm_flag" VARCHAR(10) DEFAULT '正常';

COMMENT ON COLUMN "BQZM"."lighting_circuit"."alarm_flag" IS '是否报警（报警/正常）';
