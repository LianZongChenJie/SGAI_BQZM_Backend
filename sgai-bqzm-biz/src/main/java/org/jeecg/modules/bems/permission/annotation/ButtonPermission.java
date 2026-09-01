package org.jeecg.modules.bems.permission.annotation;

import java.lang.annotation.*;

/**
 * 按钮权限注解
 * 标记需要校验按钮/操作权限的方法，由 {@code ButtonPermissionAspect} 切面拦截。
 *
 * 用法：
 * <pre>
 * @ButtonPermission("system:user:edit")          // 需拥有该按钮权限码
 * public Result&lt;String&gt; controlAll() { ... }
 * </pre>
 *
 * 权限码即 jeecg 权限表 sys_permission.perms（与前端 allAuth 中权限集合一致）。
 * 切面按当前登录用户的角色查询其拥有的权限码，命中即放行；否则抛 JeecgBootException。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ButtonPermission {
    /**
     * 按钮权限码（sys_permission.perms），如 system:user:edit
     */
    String value();
}
