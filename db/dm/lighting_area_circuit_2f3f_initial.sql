-- =====================================================================
-- 2号馆 / 3号馆 / 其他建筑 区域+回路 初始化数据
-- 数据来源：用户提供 18 行基础信息（每行 = 一个回路 = 一个区域，区域下仅含 1 个回路）
-- 规则：
--   * space 统一 = 905
--   * 区域类别 relName = 泛光照明
--   * spaceName：2AL前缀 → 2号馆；3AL前缀 → 3号馆；其他按回路名称取对应位置名
--   * 回路名 circuitName = 第 2 列；回路编码 circuitCode = 第 3 列设备号
--   * 区域编码 areaCode = 第 1 列位置编码；区域名 areaName = 回路名（同名时加设备号后缀）
--   * 片区：不新增，全部归入现有【服贸会区】district_id = 8
--   * id：不显式指定，从当前表 MAX(id)+1 起按插入顺序递增（ROWNUM 生成）
--   * 回路：保存 area_id 关联到上面插入的对应区域（按 area_name + space 匹配）
-- 数据库：达梦 DM8
-- 幂等：重复执行前请先删除旧数据（见文件末尾 DELETE 语句）
-- =====================================================================

-- ---------- 1. 插入 17 条区域（id = 当前 MAX(id)+1 起递增，归入服贸会区 district_id=8） ----------
INSERT INTO lighting_area (id, space, space_name, area_name, area_code, rel_name, district_id, type, circuit_count, online_count, status, map_level, sort)
SELECT (SELECT ISNULL(MAX(id), 0) FROM lighting_area) + ROWNUM,
       t.space, t.space_name, t.area_name, t.area_code, t.rel_name, t.district_id, t.type,
       t.circuit_count, t.online_count, t.status, t.map_level, t.sort
FROM (
    SELECT '905' AS space, '2号馆' AS space_name, '2#B1F总控'   AS area_name, '2AL-B1F-1'  AS area_code, '泛光照明' AS rel_name, 8 AS district_id, '1' AS type, 1 AS circuit_count, 1 AS online_count, '启用' AS status, 1 AS map_level, 1  AS sort FROM DUAL
    UNION ALL SELECT '905', '2号馆', '2#1F总控',    '2AL-1F-2',   '泛光照明', 8, '1', 1, 1, '启用', 1, 2  FROM DUAL
    UNION ALL SELECT '905', '2号馆', '2#2F总控',    '2AL-2F-1',   '泛光照明', 8, '1', 1, 1, '启用', 1, 3  FROM DUAL
    UNION ALL SELECT '905', '2号馆', '2#屋顶总控',  '2AL-RF-1',   '泛光照明', 8, '1', 1, 1, '启用', 1, 4  FROM DUAL
    UNION ALL SELECT '905', '2号馆', '场景控制(36)','2AL-1F-2',   '泛光照明', 8, '1', 1, 1, '启用', 1, 5  FROM DUAL
    UNION ALL SELECT '905', '2号馆', '场景控制(37)','2AL-1F-2',   '泛光照明', 8, '1', 1, 1, '启用', 1, 6  FROM DUAL
    UNION ALL SELECT '905', '2号馆', '场景控制(38)','2AL-1F-2',   '泛光照明', 8, '1', 1, 1, '启用', 1, 7  FROM DUAL
    UNION ALL SELECT '905', '3号馆', '3#1F总控',    '3AL-1F-1',   '泛光照明', 8, '1', 1, 1, '启用', 1, 8  FROM DUAL
    UNION ALL SELECT '905', '3号馆', '3#2F总控',    '3AL-2F-1',   '泛光照明', 8, '1', 1, 1, '启用', 1, 9  FROM DUAL
    UNION ALL SELECT '905', '3号馆', '3#3F总控',    '3AL-3F-1',   '泛光照明', 8, '1', 1, 1, '启用', 1, 10 FROM DUAL
    UNION ALL SELECT '905', '3号馆', '3#屋顶总控',  '3AL-RF-1',   '泛光照明', 8, '1', 1, 1, '启用', 1, 11 FROM DUAL
    UNION ALL SELECT '905', '一焦炉',      '一焦炉总控',   '一焦炉AL-JG4', '泛光照明', 8, '1', 1, 1, '启用', 1, 12 FROM DUAL
    UNION ALL SELECT '905', '西会议楼',    '西会议楼总控', 'AL-2#ZF',      '泛光照明', 8, '1', 1, 1, '启用', 1, 13 FROM DUAL
    UNION ALL SELECT '905', '制粉车间',    '制粉车间总控', 'AL-2#ZF',      '泛光照明', 8, '1', 1, 1, '启用', 1, 14 FROM DUAL
    UNION ALL SELECT '905', '标识',        '标识总控',     '标识WDH-AP1',  '泛光照明', 8, '1', 1, 1, '启用', 1, 15 FROM DUAL
    UNION ALL SELECT '905', '35米灯杆',    '35米灯杆总控', '35米灯杆AL-JG2','泛光照明', 8, '1', 1, 1, '启用', 1, 16 FROM DUAL
    UNION ALL SELECT '905', '景观照明',    '景观照明总控', '景观照明AL-JG3','泛光照明', 8, '1', 1, 1, '启用', 1, 17 FROM DUAL
) t;

-- ---------- 2. 插入 17 条回路（id = 当前 MAX(id)+1 起递增，area_id 关联对应区域） ----------
INSERT INTO lighting_circuit (id, circuit_name, circuit_code, area_id, status, comstat)
SELECT (SELECT ISNULL(MAX(id), 0) FROM lighting_circuit) + ROWNUM,
       t.circuit_name, t.circuit_code,
       (SELECT a.id FROM lighting_area a WHERE a.area_name = t.area_name AND a.space = '905'),
       t.status, t.comstat
FROM (
    SELECT '2#B1F总控'   AS circuit_name, '42' AS circuit_code, '2#B1F总控'   AS area_name, '关闭' AS status, '在线' AS comstat FROM DUAL
    UNION ALL SELECT '2#1F总控',    '34', '2#1F总控',    '关闭', '在线' FROM DUAL
    UNION ALL SELECT '2#2F总控',    '17', '2#2F总控',    '关闭', '在线' FROM DUAL
    UNION ALL SELECT '2#屋顶总控',  '26', '2#屋顶总控',  '关闭', '在线' FROM DUAL
    UNION ALL SELECT '场景控制(36)','36', '场景控制(36)','关闭', '在线' FROM DUAL
    UNION ALL SELECT '场景控制(37)','37', '场景控制(37)','关闭', '在线' FROM DUAL
    UNION ALL SELECT '场景控制(38)','38', '场景控制(38)','关闭', '在线' FROM DUAL
    UNION ALL SELECT '3#1F总控',    '3',  '3#1F总控',    '关闭', '在线' FROM DUAL
    UNION ALL SELECT '3#2F总控',    '50', '3#2F总控',    '关闭', '在线' FROM DUAL
    UNION ALL SELECT '3#3F总控',    '56', '3#3F总控',    '关闭', '在线' FROM DUAL
    UNION ALL SELECT '3#屋顶总控',  '59', '3#屋顶总控',  '关闭', '在线' FROM DUAL
    UNION ALL SELECT '一焦炉总控',   '23', '一焦炉总控',   '关闭', '在线' FROM DUAL
    UNION ALL SELECT '西会议楼总控', '9',  '西会议楼总控', '关闭', '在线' FROM DUAL
    UNION ALL SELECT '制粉车间总控', '10', '制粉车间总控', '关闭', '在线' FROM DUAL
    UNION ALL SELECT '标识总控',     '47', '标识总控',     '关闭', '在线' FROM DUAL
    UNION ALL SELECT '35米灯杆总控', '63', '35米灯杆总控', '关闭', '在线' FROM DUAL
    UNION ALL SELECT '景观照明总控', '31', '景观照明总控', '关闭', '在线' FROM DUAL
) t;

-- ---------- 3. 更新服贸会区（district_id=8）的片区统计 ----------
UPDATE lighting_district d
   SET d.circuit_count = (SELECT COUNT(*) FROM lighting_area a WHERE a.district_id = d.id),
       d.online_count  = (SELECT COUNT(*) FROM lighting_area a WHERE a.district_id = d.id AND a.status = '启用')
 WHERE d.id = 8;

-- ---------- 4. 验证查询 ----------
-- 新增区域回路一对一检查
SELECT a.id, a.area_name, a.space_name, c.circuit_name, c.circuit_code, c.area_id
  FROM lighting_area a
  LEFT JOIN lighting_circuit c ON c.area_id = a.id
 WHERE a.space = '905'
 ORDER BY a.id;

-- ---------- 幂等清理（重复执行前取消注释运行） ----------
-- DELETE FROM lighting_circuit WHERE area_id IN (SELECT id FROM lighting_area WHERE space = '905');
-- DELETE FROM lighting_area WHERE space = '905';