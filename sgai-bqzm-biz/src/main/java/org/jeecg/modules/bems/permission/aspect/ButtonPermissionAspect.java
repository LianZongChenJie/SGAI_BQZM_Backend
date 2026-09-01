package org.jeecg.modules.bems.permission.aspect;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.bems.permission.annotation.ButtonPermission;
import org.jeecg.modules.bems.permission.mapper.ButtonPermissionMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 按钮权限AOP切面
 * 拦截带 {@link ButtonPermission} 注解的方法，校验当前登录用户是否拥有指定按钮权限码。
 *
 * 权限码来源：按当前用户角色关联查询 jeecg 系统表 sys_permission.perms
 * （与前端接口 /sys/permission/getUserPermissionByToken 返回的权限集合一致）。
 *
 * 校验规则：
 * - 用户权限码集合包含注解指定的权限码 → 放行
 * - 未登录 / 权限码为空 / 不包含指定权限码 → 抛 JeecgBootException
 */
@Aspect
@Component
@Order(1)
@Slf4j
public class ButtonPermissionAspect {

    @Resource
    private ButtonPermissionMapper buttonPermissionMapper;

    @PostConstruct
    public void init() {
        log.info(">>> ButtonPermissionAspect 已加载（按钮权限切面生效），mapper={}", buttonPermissionMapper != null);
    }

    /**
     * 环绕通知：拦截带 @ButtonPermission 注解的方法
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(org.jeecg.modules.bems.permission.annotation.ButtonPermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取方法签名和注解
        log.info(">>> ButtonPermissionAspect 触发拦截: method={}", joinPoint.getSignature().toShortString());
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ButtonPermission annotation = method.getAnnotation(ButtonPermission.class);
        String requiredPerm = annotation.value();

        // 2. 获取当前登录用户
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (sysUser == null) {
            log.warn("未登录用户，禁止访问需要按钮权限的方法: method={}, perm={}", method.getName(), requiredPerm);
            throw new JeecgBootException("未登录或登录已过期，无操作权限");
        }

        // 3. 查询当前用户拥有的权限码集合
        boolean hasPermission = false;
        try {
            List<String> userPerms = buttonPermissionMapper.selectPermsByUserId(sysUser.getId());
            if (userPerms != null) {
                for (String perm : userPerms) {
                    if (requiredPerm.equals(perm)) {
                        hasPermission = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // 查询权限失败时按无权限处理，避免绕过
            log.error("查询用户按钮权限失败: user={}, perm={}, err={}", sysUser.getUsername(), requiredPerm, e.getMessage());
            throw new JeecgBootException("权限校验异常，请联系管理员");
        }

        // 4. 无权限则抛出业务异常
        if (!hasPermission) {
            log.warn("用户无按钮权限: user={}, requiredPerm={}", sysUser.getUsername(), requiredPerm);
            throw new JeecgBootException("无操作权限，请联系管理员分配权限【" + requiredPerm + "】");
        }

        // 5. 有权限则执行原方法
        return joinPoint.proceed();
    }
}
