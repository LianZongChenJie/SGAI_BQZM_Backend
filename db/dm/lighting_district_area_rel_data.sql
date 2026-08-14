-- =====================================================================
-- 片区-分组-区域关联表演示数据（lighting_district_area_rel）
-- 片区：8-服贸会区（district_id = 8）
-- 分组：AAAA / BBBB / CCCC，按 area.sort 升序把 903 区域的 28 条数据均分成三组
--   AAAA = sort 3~14（10 条）  BBBB = sort 15~23（9 条）  CCCC = sort 24~33（9 条）
-- 数据库：达梦 DM8；需先执行 lighting_district_area_rel.sql 建表
-- 幂等：重复执行前请先删除旧数据（DELETE FROM lighting_district_area_rel WHERE district_id = 8;）
-- =====================================================================

-- ========== 分组 AAAA（sort 3~14）==========
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'AAAA', 452, '南配电段高线', 1, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'AAAA', 451, '南配电小路灯', 2, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'AAAA', 449, '南配电南侧绿地泛光', 3, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'AAAA', 450, '南配电南侧绿地树灯', 4, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'AAAA', 453, '11号馆西侧红变形金刚朝西', 5, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'AAAA', 454, '四五焦炉和烟囱', 6, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'AAAA', 455, '十号馆泛光', 7, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'AAAA', 456, '绿轴中段高线', 8, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'AAAA', 457, '十号馆北侧绿地', 9, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'AAAA', 458, '绿洲北段高线', 10, '演示分组', 'admin', CURRENT_TIMESTAMP);

-- ========== 分组 BBBB（sort 15~23）==========
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'BBBB', 470, '修理车间A泛光', 1, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'BBBB', 471, '修理车间B泛光', 2, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'BBBB', 468, '刀具车间西侧绿地', 3, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'BBBB', 467, '刀具车间泛光', 4, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'BBBB', 469, '修理车间C和LOGO泛光', 5, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'BBBB', 466, '首钢大食堂泛光', 6, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'BBBB', 464, '四五焦虑一层红光', 7, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'BBBB', 465, '四五焦炉一层照明', 8, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'BBBB', 462, '三焦炉顶部泛光', 9, '演示分组', 'admin', CURRENT_TIMESTAMP);

-- ========== 分组 CCCC（sort 24~33）==========
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'CCCC', 459, '北初冷泛光', 1, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'CCCC', 460, '旗阵绿地', 2, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'CCCC', 463, '绿轴广场烟囱高灯光', 3, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'CCCC', 461, '三焦炉卫生间泛光', 4, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'CCCC', 448, '南转运站泛光', 5, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'CCCC', 445, '脱硫车间泛光', 6, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'CCCC', 446, '厂东门泛光', 7, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'CCCC', 444, '脱硫车间南侧绿地', 8, '演示分组', 'admin', CURRENT_TIMESTAMP);
INSERT INTO "BQZM"."lighting_district_area_rel" ("district_id", "group_name", "area_id", "area_name", "sort", "remark", "create_by", "create_time") VALUES (8, 'CCCC', 447, '空中步道南端烟囱泛光', 9, '演示分组', 'admin', CURRENT_TIMESTAMP);
