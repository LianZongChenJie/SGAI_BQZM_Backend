-- ============================================================
-- lighting_area 表增加"是否有箱子"字段
-- 值：'有' / '无'，默认 '无'
-- 说明：表里没有 has_box 列时执行一次即可；已存在则跳过
-- 数据库：达梦 DM8
-- ============================================================

ALTER TABLE "BQZM"."lighting_area" ADD "has_box" VARCHAR(10) DEFAULT '无';

COMMENT ON COLUMN "BQZM"."lighting_area"."has_box" IS '是否有箱子（有/无）';
