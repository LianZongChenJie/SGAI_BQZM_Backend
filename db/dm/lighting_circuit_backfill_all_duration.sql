-- ============================================================
-- lighting_circuit 表回填"开启总时长"（all_duration，单位：秒）
-- 背景：历史数据 all_duration 为空/0，但 start_time、closing_time 有值
--       （如 09:31:55 → 09:33:30 的周期，all_duration 却是 0）
-- 说明：lighting_circuit 只保留最近一次开关周期的 start_time/closing_time，
--       因此本脚本回填的是最近一次开关周期的时长（无法还原更早的多轮开关历史，
--       更早的周期数据未留存）。回填后新的开关周期由代码正常累计（关闭时累加）。
-- 只补 all_duration 为空或 0 的回路，已有累计值的不动；可重复执行（幂等）。
-- ============================================================

UPDATE "BQZM"."lighting_circuit"
SET all_duration = DATEDIFF(SECOND, start_time, closing_time)
WHERE (all_duration IS NULL OR all_duration = 0)
  AND start_time IS NOT NULL
  AND closing_time IS NOT NULL
  AND closing_time > start_time;
