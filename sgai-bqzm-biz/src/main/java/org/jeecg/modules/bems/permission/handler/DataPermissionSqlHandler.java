package org.jeecg.modules.bems.permission.handler;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import org.jeecg.modules.bems.permission.holder.DataPermissionHolder;
import org.jeecg.modules.bems.permission.vo.UserDataScope;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 数据权限SQL处理器
 * 负责构建权限过滤的SQL条件表达式
 *
 * @author bems
 * @date 2026-03-12
 */
@Slf4j
@Component
public class DataPermissionSqlHandler {

    /**
     * 构建权限条件表达式（动态版本）
     *
     * @param tableName 表名
     * @param fieldMappings 权限字段映射 Map<权限类型, 数据库字段名>
     * @return 权限条件表达式
     */
    public Expression buildPermissionExpression(String tableName, Map<String, String> fieldMappings) {
        UserDataScope dataScope = DataPermissionHolder.getDataScope();
        if (dataScope == null || fieldMappings == null || fieldMappings.isEmpty()) {
            return null;
        }

        List<Expression> expressions = new ArrayList<>();

        // 遍历所有权限类型，动态构建条件
        for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
            String permissionType = entry.getKey();        // 如 "CATEGORY", "SPACE"
            String fieldName = entry.getValue();           // 如 "category_id", "space_id"

            // 获取该类型的权限ID集合
            Set<Long> ids = dataScope.getPermissionIds(permissionType);

            // 如果有权限数据，构建 IN 表达式
            if (ids != null && !ids.isEmpty()) {
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(new Column(tableName + "." + fieldName));

                List<Expression> valueList = new ArrayList<>();
                for (Long id : ids) {
                    valueList.add(new LongValue(id));
                }
                inExpression.setRightItemsList(new ExpressionList(valueList));

                expressions.add(inExpression);

                log.debug("构建权限条件: type={}, field={}, count={}",
                         permissionType, fieldName, ids.size());
            }
            // 没有权限数据时不加条件，放行查所有
        }

        // 没有任何权限条件，直接返回null，不加限制
        if (expressions.isEmpty()) {
//            log.info("无可用权限数据，不加权限限制: tableName={}", tableName);
            return null;
        }

        // 使用 AND 连接多个条件（用户必须同时满足所有权限条件）
        Expression result = expressions.get(0);
        for (int i = 1; i < expressions.size(); i++) {
            result = new AndExpression(result, expressions.get(i));
        }

        // 使用括号包围 AND 表达式以确保正确的优先级
        result = new Parenthesis(result);

        log.info("构建权限条件成功: tableName={}, conditionCount={}", tableName, expressions.size());

        return result;
    }

    /**
     * 保留旧方法以保持兼容性（可选）
     * @deprecated 使用 {@link #buildPermissionExpression(String, Map)} 替代
     */
    @Deprecated
    public Expression buildPermissionExpression(String tableName, String categoryField, String spaceField) {
        Map<String, String> fieldMappings = new HashMap<>();
        if (categoryField != null) {
            fieldMappings.put("CATEGORY", categoryField);
        }
        if (spaceField != null) {
            fieldMappings.put("SPACE", spaceField);
        }
        return buildPermissionExpression(tableName, fieldMappings);
    }
}