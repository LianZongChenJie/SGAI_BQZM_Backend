-- ----------------------------------------------------------------------------
-- lighting_scene 表新增标签字段（标签name、标签id）
-- 与 group_id 风格一致，VARCHAR(100)
-- ----------------------------------------------------------------------------
ALTER TABLE "BQZM"."lighting_scene" ADD "tag_id" VARCHAR(100);
ALTER TABLE "BQZM"."lighting_scene" ADD "tag_name" VARCHAR(100);

COMMENT ON COLUMN "BQZM"."lighting_scene"."tag_id" IS '标签ID';
COMMENT ON COLUMN "BQZM"."lighting_scene"."tag_name" IS '标签名称';
