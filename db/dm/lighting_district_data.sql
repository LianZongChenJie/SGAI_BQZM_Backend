-- =====================================================================
-- 片区表初始化数据（基于 lighting_area 现有数据生成）
-- 分组规则：按 space（空间）分组，共 6 个片区，覆盖全部 441 条区域数据
--   1-金安桥  2-一高炉  3-制氧南  4-制氧北  901-四高炉  902-1号馆
-- 数据库：达梦 DM8
-- =====================================================================

-- 1. 插入片区（不指定 id，由自增列生成）
INSERT INTO lighting_district (district_name, district_code, status, sort, space, space_name) VALUES
('金安桥', '1',   '启用', 1, '1',   '金安桥'),
('一高炉', '2',   '启用', 2, '2',   '一高炉'),
('制氧南', '3',   '启用', 3, '3',   '制氧南'),
('制氧北', '4',   '启用', 4, '4',   '制氧北'),
('四高炉', '901', '启用', 5, '901', '四高炉'),
('1号馆',  '902', '启用', 6, '902', '1号馆');

-- 2. 把区域关联到片区（lighting_area.district_id = 对应片区 id，按 space 匹配）
UPDATE lighting_area a
   SET a.district_id = (SELECT d.id FROM lighting_district d WHERE d.space = a.space);

-- 3.（可选）汇总片区运行时长：各片区下所有区域 all_duration 之和
UPDATE lighting_district d
   SET d.all_duration = (SELECT SUM(COALESCE(a.all_duration, 0))
                         FROM lighting_area a
                         WHERE a.space = d.space);

-- 4. 验证：查询片区下区域数量
SELECT d.id, d.district_name, d.district_code, d.space, COUNT(a.id) AS area_count
  FROM lighting_district d
  LEFT JOIN lighting_area a ON a.district_id = d.id
 GROUP BY d.id, d.district_name, d.district_code, d.space
 ORDER BY d.sort;
