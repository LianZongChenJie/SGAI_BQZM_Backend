package org.jeecg.modules.bems.handler;


import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * 适配达梦数据库的 LocalDate 类型处理器
 */
@MappedJdbcTypes(value = JdbcType.DATE,includeNullJdbcType = true)
@MappedTypes({LocalDate.class})
public class DmLocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType) throws SQLException {
        // 将 LocalDate 转换为 Date 以适配达梦数据库
        ps.setDate(i, Date.valueOf(parameter));
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Date date = rs.getDate(columnName);
        return date != null ? date.toLocalDate() : null;
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Date date = rs.getDate(columnIndex);
        return date != null ? date.toLocalDate() : null;
    }

    @Override
    public LocalDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Date date = cs.getDate(columnIndex);
        return date != null ? date.toLocalDate() : null;
    }
}