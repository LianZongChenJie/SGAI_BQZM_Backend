-- ============================================================
-- lighting_circuit 表增加电流字段（北区 space=903 状态消息 DataType=2 使用）
-- 表里没有 electric_current 列时执行一次即可；已存在则跳过
-- ============================================================

-- 电流（A），DECIMAL(10,2) 支持小数电流值
ALTER TABLE "BQZM"."lighting_circuit" ADD "electric_current" DECIMAL(10,2);

COMMENT ON COLUMN "BQZM"."lighting_circuit"."electric_current" IS '电流（A）';
