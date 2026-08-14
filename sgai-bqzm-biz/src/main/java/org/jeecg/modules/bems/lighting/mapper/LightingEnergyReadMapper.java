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
}
