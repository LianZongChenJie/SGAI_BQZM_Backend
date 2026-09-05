-- ============================================================
-- lighting_circuit 表增加"报警判定"记录字段
-- 说明：alarm_flag(是否报警) 已存在；此处新增命中规则及详情字段，
--       供诊断定时任务(LightingDiagJob)标记"命中了哪条规则"。
-- 数据库：达梦 DM8
-- 已存在则跳过（表里没有对应列时执行一次即可）
-- ============================================================

-- 命中的规则编码（如 R1）
ALTER TABLE "BQZM"."lighting_circuit" ADD "alarm_rule_code" VARCHAR(20);
COMMENT ON COLUMN "BQZM"."lighting_circuit"."alarm_rule_code" IS '报警命中的规则编码（R1/R2/R3/R4/R5C/R5M）';

-- 命中的规则名称（如 开路·灯具失效）
ALTER TABLE "BQZM"."lighting_circuit" ADD "alarm_rule_name" VARCHAR(50);
COMMENT ON COLUMN "BQZM"."lighting_circuit"."alarm_rule_name" IS '报警命中的规则名称';

-- 报警发生时间
ALTER TABLE "BQZM"."lighting_circuit" ADD "alarm_time" TIMESTAMP;
COMMENT ON COLUMN "BQZM"."lighting_circuit"."alarm_time" IS '报警发生时间';

-- 报警详情（如：开启状态但电流 X A 低于额定 Y A，持续 3 分钟）
ALTER TABLE "BQZM"."lighting_circuit" ADD "alarm_detail" VARCHAR(255);
COMMENT ON COLUMN "BQZM"."lighting_circuit"."alarm_detail" IS '报警详情/判定依据';
