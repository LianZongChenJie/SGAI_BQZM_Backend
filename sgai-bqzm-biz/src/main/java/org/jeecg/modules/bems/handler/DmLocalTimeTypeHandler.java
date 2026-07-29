package org.jeecg.modules.bems.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.jeecg.modules.bems.project.dto.EvaluationReportQueryDto;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;

/**
 * 适配达梦数据库的 LocalTime 类型处理器
 */
@MappedJdbcTypes(value = JdbcType.TIME, includeNullJdbcType = true)
@MappedTypes(LocalTime.class)
public class DmLocalTimeTypeHandler extends BaseTypeHandler<LocalTime> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalTime parameter, JdbcType jdbcType) throws SQLException {
        // 将 LocalTime 转换为 Time 以适配达梦数据库
        ps.setTime(i, Time.valueOf(parameter));
    }

    @Override
    public LocalTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Time time = rs.getTime(columnName);
        return time != null ? time.toLocalTime() : null;
    }

    @Override
    public LocalTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Time time = rs.getTime(columnIndex);
        return time != null ? time.toLocalTime() : null;
    }

    @Override
    public LocalTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Time time = cs.getTime(columnIndex);
        return time != null ? time.toLocalTime() : null;
    }
}