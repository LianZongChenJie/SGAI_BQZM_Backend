package org.jeecg.modules.bems.permission.annotation;

import java.lang.annotation.*;

/**
 * 数据权限字段注解
 * 标记实体类中需要进行权限过滤的字段
 *
 * <p>使用示例：
 * <pre>
 * &#64;DataPermissionField(type = "CATEGORY", value = "category_id")
 * private Long categoryId;
 *
 * &#64;DataPermissionField(type = "SPACE", value = "space_id")
 * private Long spaceId;
 * </pre>
 *
 * @author bems
 * @date 2026-03-12
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermissionField {

    /**
     * 权限类型
     * CATEGORY - 专业权限
     * SPACE - 空间权限
     *
     * @return 权限类型
     */
    String type();

    /**
     * 数据库字段名（不是Java属性名）
     * 例如：category_id, space_id
     *
     * @return 数据库字段名
     */
    String value();
}
