-- ----------------------------------------------------------------------------
-- 照明节目表拆分：lighting_scene 拆出 lighting_program（节目独立成表）
-- 1. 节目字段（group_id 泛光节目ID、节目名称等）迁到 lighting_program
-- 2. lighting_scene 保留场景字段 + program_scene_ids（改为关联 lighting_program.id）
-- ----------------------------------------------------------------------------

-- 1. 建节目表
CREATE TABLE "BQZM"."lighting_program" (
  "id" BIGINT NOT NULL,
  "program_name" VARCHAR(100),
  "group_id" VARCHAR(100),
  "status" VARCHAR(20) DEFAULT '启用',
  "sort" BIGINT,
  "remark" VARCHAR(500),
  "space" VARCHAR(50),
  "tag_id" VARCHAR(100),
  "tag_name" VARCHAR(100),
  "create_by" VARCHAR(50),
  "create_time" TIMESTAMP(6),
  "update_by" VARCHAR(50),
  "update_time" TIMESTAMP(6),
  "sys_org_code" VARCHAR(100)
);
COMMENT ON COLUMN "BQZM"."lighting_program"."program_name" IS '节目名称';
COMMENT ON COLUMN "BQZM"."lighting_program"."group_id" IS '泛光节目ID（关联泛光总控系统的节目）';
COMMENT ON COLUMN "BQZM"."lighting_program"."status" IS '状态：启用、禁用';
COMMENT ON COLUMN "BQZM"."lighting_program"."sort" IS '排序';
COMMENT ON COLUMN "BQZM"."lighting_program"."space" IS '所属区域（对应 lighting_area.space，如 1金安桥/2一高炉）';
COMMENT ON COLUMN "BQZM"."lighting_program"."tag_id" IS '标签ID';
COMMENT ON COLUMN "BQZM"."lighting_program"."tag_name" IS '标签名称';
COMMENT ON COLUMN "BQZM"."lighting_program"."remark" IS '备注';
ALTER TABLE "BQZM"."lighting_program" ADD PRIMARY KEY ("id");
CREATE UNIQUE INDEX "INDEX_LIGHTING_PROGRAM_ID" ON "BQZM"."lighting_program" ("id");

-- 2. 迁移数据：把 lighting_scene 里配置了泛光节目ID（group_id）的节目场景迁到节目表（id 保持不变，
--    这样 lighting_scene.program_scene_ids 里存的旧节目ID无需改动，直接关联到新节目表）
INSERT INTO "BQZM"."lighting_program" ("id","program_name","group_id","status","sort","remark","tag_id","tag_name","create_by","create_time","update_by","update_time","sys_org_code")
SELECT "id","scene_name","group_id","status","sort","remark","tag_id","tag_name","create_by","create_time","update_by","update_time","sys_org_code"
FROM "BQZM"."lighting_scene"
WHERE "group_id" IS NOT NULL AND "group_id" <> '';

-- 3. 清理：节目场景原挂在 lighting_scene_detail 的明细已无意义（节目按 groupId 控制），删除
DELETE FROM "BQZM"."lighting_scene_detail" WHERE "scene_id" IN (SELECT "id" FROM "BQZM"."lighting_program");

-- 4. 删除 lighting_scene 里的节目行（已迁到节目表，避免场景列表重复展示）
DELETE FROM "BQZM"."lighting_scene" WHERE "group_id" IS NOT NULL AND "group_id" <> '';
-- 残留的 category=节目 但没有 group_id 的行一并清理
DELETE FROM "BQZM"."lighting_scene" WHERE "category" = '节目' AND ("group_id" IS NULL OR "group_id" = '');

-- 5. 移除 lighting_scene 的 group_id 列（已迁到 lighting_program）
ALTER TABLE "BQZM"."lighting_scene" DROP COLUMN "group_id";
