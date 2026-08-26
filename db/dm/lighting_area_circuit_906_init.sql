-- =====================================================================
-- 906空间（234室外景观）区域+回路 初始化数据
-- 数据来源：234室外景观.xlsx（4 条）
-- 规则：
--   * 一条数据 = 一个回路 = 一个区域，回路表 area_id 关联对应区域
--   * space 统一 = 906
--   * area_code = GatewayCode = 154.2
--   * 回路编码 circuit_code = Excel 中的 AreaID（2、3、5、4）
--   * 区域类别 relName = 泛光照明
--   * 开关状态：1=开、2=关（区别于 905 的 0=开、1=关）
--   * 控制队列：lighting_control_bq_154_2
--   * 片区：默认归入【服贸会区】district_id=8（如需其他片区请调整）
--   * id：从当前 MAX(id)+1 起按插入顺序递增（ROWNUM 生成）
-- 数据库：达梦 DM8
-- 幂等：重复执行前请先删除旧数据（见文件末尾 DELETE 语句）
-- =====================================================================

-- ---------- 1. 插入 4 条区域（space=906，area_code=154.2，id 递增） ----------
INSERT INTO lighting_area (id, space, space_name, area_name, area_code, rel_name, district_id, type, circuit_count, online_count, status, map_level, sort)
SELECT (SELECT ISNULL(MAX(id), 0) FROM lighting_area) + ROWNUM,
       t.space, t.space_name, t.area_name, t.area_code, t.rel_name, t.district_id, t.type,
       t.circuit_count, t.online_count, t.status, t.map_level, t.sort
FROM (
    SELECT '906' AS space, '234室外景观' AS space_name, '2号馆总控'     AS area_name, '154.2' AS area_code, '泛光照明' AS rel_name, 8 AS district_id, '1' AS type, 1 AS circuit_count, 1 AS online_count, '启用' AS status, 1 AS map_level, 1 AS sort FROM DUAL
    UNION ALL SELECT '906', '234室外景观', '3号馆总控',     '154.2', '泛光照明', 8, '1', 1, 1, '启用', 1, 2 FROM DUAL
    UNION ALL SELECT '906', '234室外景观', '4号馆总控',     '154.2', '泛光照明', 8, '1', 1, 1, '启用', 1, 3 FROM DUAL
    UNION ALL SELECT '906', '234室外景观', '景观照明总控',   '154.2', '泛光照明', 8, '1', 1, 1, '启用', 1, 4 FROM DUAL
) t;

-- ---------- 2. 插入 4 条回路（id 递增，circuit_code=Excel AreaID，area_id 关联对应区域） ----------
INSERT INTO lighting_circuit (id, circuit_name, circuit_code, area_id, status, comstat)
SELECT (SELECT ISNULL(MAX(id), 0) FROM lighting_circuit) + ROWNUM,
       t.circuit_name, t.circuit_code,
       (SELECT a.id FROM lighting_area a WHERE a.area_name = t.area_name AND a.space = '906'),
       t.status, t.comstat
FROM (
    SELECT '2号馆总控'   AS circuit_name, '2' AS circuit_code, '2号馆总控'   AS area_name, '关闭' AS status, '在线' AS comstat FROM DUAL
    UNION ALL SELECT '3号馆总控',     '3', '3号馆总控',     '关闭', '在线' FROM DUAL
    UNION ALL SELECT '4号馆总控',     '5', '4号馆总控',     '关闭', '在线' FROM DUAL
    UNION ALL SELECT '景观照明总控',  '4', '景观照明总控',  '关闭', '在线' FROM DUAL
) t;

-- ---------- 3. 更新服贸会区（district_id=8）的片区统计 ----------
UPDATE lighting_district d
   SET d.circuit_count = (SELECT COUNT(*) FROM lighting_area a WHERE a.district_id = d.id),
       d.online_count  = (SELECT COUNT(*) FROM lighting_area a WHERE a.district_id = d.id AND a.status = '启用')
 WHERE d.id = 8;

-- ---------- 4. 验证查询 ----------
-- 新增区域回路一对一检查
SELECT a.id, a.area_name, a.space_name, a.area_code, c.circuit_name, c.circuit_code, c.area_id
  FROM lighting_area a
  LEFT JOIN lighting_circuit c ON c.area_id = a.id
 WHERE a.space = '906'
 ORDER BY a.id;

-- ---------- 幂等清理（重复执行前取消注释运行） ----------
-- DELETE FROM lighting_circuit WHERE area_id IN (SELECT id FROM lighting_area WHERE space = '906');
-- DELETE FROM lighting_area WHERE space = '906';