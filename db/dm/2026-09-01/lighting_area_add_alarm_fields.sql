-- ============================================================
-- lighting_area 表增加"是否报警"、"报警数量"字段
-- 说明：表里没有对应列时执行一次即可；已存在则跳过
-- 数据库：达梦 DM8
-- ============================================================

ALTER TABLE "BQZM"."lighting_area" ADD "alarm_flag" VARCHAR(10) DEFAULT '正常';
ALTER TABLE "BQZM"."lighting_area" ADD "alarm_count" INT DEFAULT 0;

COMMENT ON COLUMN "BQZM"."lighting_area"."alarm_flag" IS '是否报警（报警/正常）';
COMMENT ON COLUMN "BQZM"."lighting_area"."alarm_count" IS '报警数量';
