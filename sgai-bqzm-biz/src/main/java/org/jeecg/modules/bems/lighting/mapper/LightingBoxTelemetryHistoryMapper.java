package org.jeecg.modules.bems.lighting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetryHistory;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LightingBoxTelemetryHistoryMapper extends BaseMapper<LightingBoxTelemetryHistory> {

    /**
     * 查询某时间窗口内有累计电量的网关去重集合（用于小时聚合/区间抄表）
     * 可传入 areaIds 按区域过滤（为空时不加区域条件）
     */
    @Select("<script>" +
            "SELECT DISTINCT gateway_code FROM lighting_box_telemetry_history " +
            "WHERE collect_time &gt;= #{start} AND collect_time &lt; #{end} " +
            "AND gateway_code IS NOT NULL AND total_energy IS NOT NULL " +
            "<if test='areaIds != null and areaIds.size() &gt; 0'>" +
            "AND area_id IN <foreach collection='areaIds' item='aid' open='(' separator=',' close=')'>#{aid}</foreach>" +
            "</if>" +
            "</script>")
    List<LightingBoxTelemetryHistory> selectDistinctGateways(@Param("start") LocalDateTime start,
                                                             @Param("end") LocalDateTime end,
                                                             @Param("areaIds") List<Long> areaIds);

    /**
     * 查询指定时间之前最近的一条箱子遥测（按网关，用于取累计电量基准值）
     */
    @Select("SELECT TOP 1 * FROM lighting_box_telemetry_history " +
            "WHERE gateway_code = #{gateway} AND collect_time < #{time} " +
            "AND total_energy IS NOT NULL ORDER BY collect_time DESC")
    LightingBoxTelemetryHistory selectLastBeforeByGateway(@Param("gateway") String gateway,
                                                          @Param("time") LocalDateTime time);

    /**
     * 批量查询一批网关在指定时间之前各自最近的一条累计表底（用于批量算今日累计端点，避免逐网关 N+1）
     * 利用 (gateway_code, collect_time) 联合索引
     */
    @Select("<script>" +
            "SELECT t.gateway_code, t.total_energy, t.collect_time FROM lighting_box_telemetry_history t " +
            "WHERE t.collect_time &lt; #{time} " +
            "AND t.gateway_code IN <foreach collection='gateways' item='gw' open='(' separator=',' close=')'>#{gw}</foreach> " +
            "AND t.total_energy IS NOT NULL " +
            "AND t.collect_time = (SELECT MAX(t2.collect_time) FROM lighting_box_telemetry_history t2 " +
            "                       WHERE t2.gateway_code = t.gateway_code AND t2.collect_time &lt; #{time} " +
            "                       AND t2.total_energy IS NOT NULL)" +
            "</script>")
    List<LightingBoxTelemetryHistory> selectLastBeforeByGateways(@Param("gateways") List<String> gateways,
                                                                 @Param("time") LocalDateTime time);

    /**
     * 查询时间窗口内最早的一条箱子遥测（按网关，无更早基准时近似用）
     */
    @Select("SELECT TOP 1 * FROM lighting_box_telemetry_history " +
            "WHERE gateway_code = #{gateway} AND collect_time >= #{start} AND collect_time < #{end} " +
            "AND total_energy IS NOT NULL ORDER BY collect_time ASC")
    LightingBoxTelemetryHistory selectFirstInRangeByGateway(@Param("gateway") String gateway,
                                                            @Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);
}
