package org.jeecg.modules.bems.permission.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.jeecg.modules.bems.permission.annotation.DataPermissionField;
import org.jeecg.modules.bems.permission.holder.DataPermissionHolder;
import org.jeecg.modules.bems.permission.vo.UserDataScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BEMS 数据权限处理器
 * 适配 MyBatis-Plus 标准 DataPermissionInterceptor
 *
 * <p>功能：
 * <ul>
 *   <li>实现 MultiDataPermissionHandler 接口</li>
 *   <li>自动识别实体类的权限字段（通过@DataPermissionField注解）</li>
 *   <li>从ThreadLocal获取用户权限范围</li>
 *   <li>使用DataPermissionSqlHandler构建权限条件</li>
 *   <li>返回JSQLParser Expression对象，由框架自动处理SQL修改</li>
 * </ul>
 *
 * <p>性能优化：
 * <ul>
 *   <li>字段映射缓存（避免重复反射）</li>
 *   <li>无权限数据时返回空表达式（&lt;1ms）</li>
 *   <li>无权限字段的实体类自动跳过</li>
 * </ul>
 *
 * @author bems
 * @date 2026-03-16
 */
@Slf4j
@Component
public class BemsDataPermissionHandler implements MultiDataPermissionHandler {

    @Autowired
    private DataPermissionSqlHandler sqlHandler;

    /**
     * 实体类权限字段缓存
     * Key: entityClass
     * Value: Map<权限类型, 数据库字段名>
     */
    private final ConcurrentHashMap<Class<?>, Map<String, String>> fieldMappingCache =
        new ConcurrentHashMap<>();

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        long startTime = System.nanoTime();

        try {
            log.debug(">>> 数据权限处理器触发: table={}, mappedStatementId={}",
                     table.getName(), mappedStatementId);

            // 1. 获取实体类
            Class<?> entityClass = getEntityClass(mappedStatementId);
            if (entityClass == null) {
                log.debug("无法获取实体类，跳过权限处理");
                return null;
            }

            // 2. 检查权限字段注解
            Map<String, String> fieldMapping = getPermissionFieldMapping(entityClass);
            if (fieldMapping.isEmpty()) {
                log.debug("实体类无权限字段，跳过拦截: {}", entityClass.getSimpleName());
                return null;
            }

            // 3. 获取权限数据
            UserDataScope dataScope = DataPermissionHolder.getDataScope();
            if (dataScope == null){
                log.debug("用户权限数据为空，跳过权限处理");
                return null;
            }

            // 4. 构建权限条件表达式（传递完整的字段映射）
            Expression permissionExpression = sqlHandler.buildPermissionExpression(
                table.getName(),
                fieldMapping  // 传递整个 Map，而不是硬编码的字段
            );

            if (permissionExpression == null) {
                log.warn("权限条件构建失败");
                return null;
            }

            long duration = (System.nanoTime() - startTime) / 1_000_000;
            log.info("数据权限处理完成: entityClass={}, 耗时={}ms",
                     entityClass.getSimpleName(), duration);

            return permissionExpression;

        } catch (Exception e) {
            log.error("数据权限处理器执行失败", e);
            return null;
        }
    }

    /**
     * 构建空结果表达式
     * 通过返回 "id IS NULL" 确保查询返回空结果集
     */
    private Expression buildEmptyExpression() {
        IsNullExpression nullCondition = new IsNullExpression();
        nullCondition.setLeftExpression(new Column("id"));
        return nullCondition;
    }

    /**
     * 从 mappedStatementId 提取实体类
     *
     * @param mappedStatementId 格式: com.example.mapper.UserMapper.selectById
     * @return 实体类Class对象
     */
    private Class<?> getEntityClass(String mappedStatementId) {
        String mapperClassName = mappedStatementId.substring(0, mappedStatementId.lastIndexOf('.'));

        try {
            Class<?> mapperClass = Class.forName(mapperClassName);
            java.lang.reflect.Type[] interfaces = mapperClass.getGenericInterfaces();

            for (java.lang.reflect.Type type : interfaces) {
                if (type instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.ParameterizedType pType = (java.lang.reflect.ParameterizedType) type;
                    java.lang.reflect.Type[] actualTypeArgs = pType.getActualTypeArguments();
                    if (actualTypeArgs.length > 0) {
                        return (Class<?>) actualTypeArgs[0];
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            log.warn("无法找到Mapper类: {}", mapperClassName);
        }

        return null;
    }

    /**
     * 获取权限字段映射（带缓存）
     *
     * @param entityClass 实体类
     * @return Map<权限类型(CATEGORY/SPACE), 数据库字段名>
     */
    private Map<String, String> getPermissionFieldMapping(Class<?> entityClass) {
        return fieldMappingCache.computeIfAbsent(entityClass, clazz -> {
            Map<String, String> mapping = new HashMap<>();

            for (Field declaredField : clazz.getDeclaredFields()) {
                DataPermissionField annotation = declaredField.getAnnotation(DataPermissionField.class);
                if (annotation != null) {
                    mapping.put(annotation.type(), annotation.value());
                }
            }

            log.debug("实体类权限字段映射: entityClass={}, mapping={}",
                     clazz.getSimpleName(), mapping);

            return mapping;
        });
    }

}
