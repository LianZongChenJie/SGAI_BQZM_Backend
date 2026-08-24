package org.jeecg.modules.bems.lighting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.bems.lighting.entity.LightingEnergyRead;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 照明分钟电量读数 Mapper
 */
public interface LightingEnergyReadMapper extends BaseMapper<LightingEnergyRead> {

    /**
     * 查询某时间窗口内有读数的表计（网关+回路）去重集合
     */
    @Select("SELECT DISTINCT gateway_code, circuit_code FROM lighting_energy_read " +
            "WHERE read_time >= #{start} AND read_time < #{end}")
    List<LightingEnergyRead> selectDistinctMeters(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /**
     * 查询指定时间之前最近的一条读数（用于取基准值）
     */
    @Select("SELECT TOP 1 * FROM lighting_energy_read " +
            "WHERE gateway_code = #{gateway} AND COALESCE(circuit_code,'') = COALESCE(#{circuit},'') " +
            "AND read_time < #{time} ORDER BY read_time DESC")
    LightingEnergyRead selectLastBefore(@Param("gateway") String gateway,
                                        @Param("circuit") String circuit,
                                        @Param("time") LocalDateTime time);

    /**
     * 查询时间窗口内最早的一条读数（无更早基准时近似用）
     */
    @Select("SELECT TOP 1 * FROM lighting_energy_read " +
            "WHERE gateway_code = #{gateway} AND COALESCE(circuit_code,'') = COALESCE(#{circuit},'') " +
            "AND read_time >= #{start} AND read_time < #{end} ORDER BY read_time ASC")
    LightingEnergyRead selectFirstInRange(@Param("gateway") String gateway,
                                          @Param("circuit") String circuit,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    /**
     * 查询指定时间区间内、指定区域集合下的网关级总表读数（去重网关）
     * circuit_code IS NULL 表示网关级总表读数
     */
    @Select("<script>" +
            "SELECT DISTINCT gateway_code, area_id, read_time, value FROM lighting_energy_read " +
            "WHERE read_time &gt;= #{start} AND read_time &lt;= #{end} " +
            "AND COALESCE(circuit_code,'') = '' " +
            "AND area_id IS NOT NULL " +
            "<if test='areaIds != null and areaIds.size() > 0'>" +
            "AND area_id IN " +
            "<foreach collection='areaIds' item='aid' open='(' separator=',' close=')'>#{aid}</foreach>" +
            "</if>" +
            "<if test='gateway != null and gateway != \"\"'>" +
            "AND gateway_code = #{gateway}" +
            "</if>" +
            "</script>")
    List<LightingEnergyRead> selectGatewayReads(@Param("areaIds") List<Long> areaIds,
                                                @Param("gateway") String gateway,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);
}
