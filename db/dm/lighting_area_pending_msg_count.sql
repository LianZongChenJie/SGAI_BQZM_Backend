-- ============================================================
-- lighting_area 表增加待下发消息数（MQ队列中未消费消息数）
-- 表里没有 pending_msg_count 列时执行一次即可；已存在则跳过
-- ============================================================

ALTER TABLE "BQZM"."lighting_area" ADD "pending_msg_count" INT;

COMMENT ON COLUMN "BQZM"."lighting_area"."pending_msg_count" IS '待下发消息数（MQ队列中未消费消息数）';
