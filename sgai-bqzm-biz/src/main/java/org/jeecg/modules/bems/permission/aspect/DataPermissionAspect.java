package org.jeecg.modules.bems.permission.aspect;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.bems.permission.annotation.DataPermission;
import org.jeecg.modules.bems.permission.holder.DataPermissionHolder;
import org.jeecg.modules.bems.permission.service.RoleDataPermissionService;
import org.jeecg.modules.bems.permission.vo.UserDataScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据权限AOP切面
 * 拦截带 @DataPermission 注解的方法，自动加载用户数据权限并设置到 ThreadLocal
 */
@Aspect
@Component
@Order(1)
@Slf4j
public class DataPermissionAspect {

    @Autowired
    private RoleDataPermissionService permissionService;

    /**
     * 环绕通知：拦截带 @DataPermission 注解的方法
     *
     * 执行流程：
     * 1. 获取方法签名和注解
     * 2. 检查是否启用数据权限过滤
     * 3. 获取当前登录用户
     * 4. 加载用户数据权限
     * 5. 设置到 ThreadLocal
     * 6. 执行原方法
     * 7. 清除 ThreadLocal
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(org.jeecg.modules.bems.permission.annotation.DataPermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DataPermission annotation = method.getAnnotation(DataPermission.class);

        // 2. 检查是否启用数据权限过滤
        if (!annotation.value()) {
            // 如果禁用了数据权限，直接执行原方法
            log.debug("数据权限过滤已禁用: method={}", method.getName());
            return joinPoint.proceed();
        }

        // 3. 获取当前登录用户
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (sysUser == null) {
            log.warn("未登录用户，不应用数据权限: method={}", method.getName());
            return joinPoint.proceed();
        }

        // 4. 加载当前用户数据权限
        UserDataScope dataScope = permissionService.getCurrentUserDataScope();

        // 5. 设置到 ThreadLocal
        DataPermissionHolder.setDataScope(dataScope);

        try {
            // 6. 执行原方法
            return joinPoint.proceed();
        } finally {
            // 7. 清除 ThreadLocal，避免内存泄漏
            DataPermissionHolder.clear();
        }
    }
}
