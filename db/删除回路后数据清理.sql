-- =====================================================================
-- 清理"已删除回路"产生的孤立/异常数据 SQL
-- 适用：在 lighting_circuit 表中手动删除回路后，同步清理引用该回路的异常数据
-- 库：51库 / schema=BQZM
-- 执行说明：先执行【一、检查】，确认影响范围；再执行【二、清理】；
--           日志表(lighting_control_log / lighting_operation_log)属历史留档，保留不清理。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 一、检查（先执行，确认待清理数据量）
-- ---------------------------------------------------------------------

-- 1.1 场景明细中，引用了"已删除回路"的孤立记录
SELECT count(*) AS orphan_scene_detail
FROM lighting_scene_detail
WHERE rel_type = '回路'
  AND rel_id NOT IN (SELECT id FROM lighting_circuit);

-- 1.2 具体明细（如需核对可放开注释执行）
-- SELECT sd.id, sd.scene_id, sd.rel_id, sd.rel_name, s.scene_name
-- FROM lighting_scene_detail sd
-- LEFT JOIN lighting_scene s ON s.id = sd.scene_id
-- WHERE sd.rel_type = '回路'
--   AND sd.rel_id NOT IN (SELECT id FROM lighting_circuit);

-- 1.3 计划中 rel_type='回路' 的（当前库无回路型计划；如有则需处理 rel_ids 逗号串中的回路id）
SELECT count(*) AS circuit_plan
FROM lighting_plan
WHERE rel_type = '回路';

-- ---------------------------------------------------------------------
-- 二、清理（确认无误后执行）
-- ---------------------------------------------------------------------

-- 2.1 清理场景明细中引用了已删除回路的孤立记录
DELETE FROM lighting_scene_detail
WHERE rel_type = '回路'
  AND rel_id NOT IN (SELECT id FROM lighting_circuit);

-- 2.2 若有 rel_type='回路' 的计划（1.3 结果 >0），则从 rel_ids 中剔除已删除回路的id
--     （达梦使用正则按逗号拆分后，仅保留仍存在的回路id重新拼接）
UPDATE lighting_plan
SET rel_ids = (
    SELECT LISTAGG(t.cid, ',') WITHIN GROUP (ORDER BY t.ord)
    FROM (
        SELECT
            REGEXP_SUBSTR(p.rel_ids, '[^,]+', 1, LEVEL) AS cid,
            LEVEL AS ord
        FROM lighting_plan p
        WHERE p.id = lighting_plan.id
        CONNECT BY LEVEL <= REGEXP_COUNT(p.rel_ids, ',') + 1
          AND PRIOR sys_guid() IS NOT NULL
          AND PRIOR p.id = p.id
    ) t
    WHERE t.cid IN (SELECT CAST(id AS VARCHAR2(64)) FROM lighting_circuit)
)
WHERE rel_type = '回路'
  AND rel_ids IS NOT NULL
  AND rel_ids <> '';

-- ---------------------------------------------------------------------
-- 三、收尾
-- ---------------------------------------------------------------------

-- 3.1 清理后再确认场景明细孤立记录为 0
SELECT count(*) AS orphan_scene_detail_after
FROM lighting_scene_detail
WHERE rel_type = '回路'
  AND rel_id NOT IN (SELECT id FROM lighting_circuit);

-- 3.2 提交（如达梦自动提交可忽略）
-- COMMIT;
