package org.jeecg.modules.bems.handler;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 达梦数据库(DM)专用的LocalDateTime类型处理器
 * 解决DM数据库与Java 8 LocalDateTime的兼容性问题
 */
@MappedTypes(LocalDateTime.class)
@MappedJdbcTypes(value = JdbcType.TIMESTAMP, includeNullJdbcType = true)
public class DmLocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    /**
     * 设置非空参数
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    LocalDateTime parameter, JdbcType jdbcType) throws SQLException {
        // 达梦数据库推荐使用Timestamp类型传输
        ps.setTimestamp(i, Timestamp.valueOf(parameter));
    }

    /**
     * 获取可为空的结果
     */
    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return convertToLocalDateTime(timestamp);
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnIndex);
        return convertToLocalDateTime(timestamp);
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        Timestamp timestamp = cs.getTimestamp(columnIndex);
        return convertToLocalDateTime(timestamp);
    }

    /**
     * 将Timestamp转换为LocalDateTime
     * 处理达梦数据库可能返回的特殊时间格式
     */
    private LocalDateTime convertToLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        // 达梦数据库有时会返回带时区的Timestamp，需要特殊处理
        return LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
    }
}