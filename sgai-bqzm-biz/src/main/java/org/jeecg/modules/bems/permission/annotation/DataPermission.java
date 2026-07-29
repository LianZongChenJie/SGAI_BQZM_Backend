package org.jeecg.modules.bems.permission.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 标记需要应用数据权限过滤的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {
    /**
     * 是否启用数据权限过滤
     */
    boolean value() default true;
}
