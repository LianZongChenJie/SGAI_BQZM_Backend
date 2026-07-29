package org.jeecg.modules.bems.permission.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.bems.permission.config.DataPermissionCacheManager;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;
import org.jeecg.modules.bems.permission.mapper.RoleDataPermissionMapper;
import org.jeecg.modules.bems.permission.service.RoleDataPermissionService;
import org.jeecg.modules.bems.permission.vo.UserDataScope;
import org.jeecg.common.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色数据权限服务实现
 */
@Service
@Slf4j
public class RoleDataPermissionServiceImpl extends ServiceImpl<RoleDataPermissionMapper, RoleDataPermission>
        implements RoleDataPermissionService {

    // 缓存键前缀
    private static final String CACHE_KEY_PREFIX = "dataPermission:";

    // 缓存过期时间（秒）- 与版本管理器保持一致
    private static final long CACHE_EXPIRE_SECONDS = 300;

    // 缓存键分隔符
    private static final String CACHE_KEY_SEPARATOR = ":";

    @Autowired
    private DataPermissionCacheManager cacheManager;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 获取当前登录用户的用户名
     *
     * @return 用户名，如果未登录或获取失败则返回 null
     */
    private String getCurrentUserId() {
        try {
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            if (sysUser == null) {
                log.debug("获取用户ID失败，用户未登录");
                return null;
            }
            return sysUser.getUsername(); // 使用用户名作为用户标识
        } catch (Exception e) {
            log.error("获取当前用户ID异常", e);
            return null;
        }
    }

    /**
     * 生成当前用户权限缓存键
     * 格式: dataPermission:currentUser:{userId}:{version}
     *
     * @param version 版本号
     * @return 缓存键
     */
    private String getCurrentUserCacheKey(long version) {
        String userId = getCurrentUserId();
        if (StrUtil.isEmpty(userId)) {
            // 未登录用户使用匿名标识（实际上不会缓存，因为方法会提前返回）
            userId = "anonymous";
        }
        return CACHE_KEY_PREFIX + "currentUser" + CACHE_KEY_SEPARATOR +
               userId + CACHE_KEY_SEPARATOR + version;
    }

    /**
     * 生成角色权限缓存键
     * 格式: dataPermission:role:{roleCode}:{version}
     */
    private String getRoleCacheKey(String roleCode, long version) {
        return CACHE_KEY_PREFIX + "role" + CACHE_KEY_SEPARATOR +
               roleCode + CACHE_KEY_SEPARATOR + version;
    }

    @Override
    public UserDataScope getCurrentUserDataScope() {
        // 1. 获取当前版本号
        long version = cacheManager.getCurrentVersion();

        // 2. 构建缓存键
        String cacheKey = getCurrentUserCacheKey(version);

        // 3. 尝试从缓存获取
        try {
            Object cachedData = redisUtil.get(cacheKey);
            if (cachedData != null) {
                log.debug("从缓存获取当前用户权限: version={}", version);
                return (UserDataScope) cachedData;
            }
        } catch (Exception e) {
            log.warn("从缓存获取用户权限失败，继续查询数据库", e);
        }

        // 4. 缓存未命中，执行原有业务逻辑
        UserDataScope dataScope = new UserDataScope();

        // 获取当前登录用户的所有角色
        Set<String> roleCodes = getCurrentUserRoles();

        if (CollUtil.isEmpty(roleCodes)) {
            log.debug("当前用户无角色，返回空权限");
            return dataScope;
        }

        // 查询角色权限
        List<RoleDataPermission> permissions = baseMapper.selectList(
            new LambdaQueryWrapper<RoleDataPermission>()
                .in(RoleDataPermission::getRoleCode, roleCodes)
        );

        fillDataScope(dataScope, permissions);
        log.debug("当前用户权限加载完成: {}", dataScope);

        // 5. 回填缓存
        try {
            redisUtil.set(cacheKey, dataScope, CACHE_EXPIRE_SECONDS);
            log.debug("当前用户权限已缓存: key={}", cacheKey);
        } catch (Exception e) {
            log.warn("缓存当前用户权限失败", e);
        }

        return dataScope;
    }

    @Override
    @Transactional
    public void assignPermission(String roleCode, String permissionType, Long resourceId) {
        // 检查是否已存在
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<RoleDataPermission>()
                .eq(RoleDataPermission::getRoleCode, roleCode)
                .eq(RoleDataPermission::getPermissionType, permissionType)
                .eq(RoleDataPermission::getResourceId, resourceId)
        );

        if (count > 0) {
            log.warn("角色已有该权限: roleCode={}, type={}, resourceId={}", roleCode, permissionType, resourceId);
            return;
        }

        RoleDataPermission permission = new RoleDataPermission();
        permission.setRoleCode(roleCode);
        permission.setPermissionType(permissionType);
        permission.setResourceId(resourceId);

        baseMapper.insert(permission);
        log.info("为角色分配数据权限: roleCode={}, type={}, resourceId={}", roleCode, permissionType, resourceId);
        // 更新缓存版本号，使旧缓存失效
        cacheManager.updateCacheVersion();
    }

    @Override
    @Transactional
    public void batchAssignPermission(String roleCode, String permissionType, Set<Long> resourceIds) {
        // 1. 先清空该角色在该类型下的所有权限
        baseMapper.delete(
            new LambdaQueryWrapper<RoleDataPermission>()
                .eq(RoleDataPermission::getRoleCode, roleCode)
                .eq(RoleDataPermission::getPermissionType, permissionType)
        );

        // 2. 如果 resourceIds 不为空，批量插入新权限
        if (CollUtil.isNotEmpty(resourceIds)) {
            for (Long resourceId : resourceIds) {
                RoleDataPermission permission = new RoleDataPermission();
                permission.setRoleCode(roleCode);
                permission.setPermissionType(permissionType);
                permission.setResourceId(resourceId);
                baseMapper.insert(permission);
            }
            log.info("批量为角色分配数据权限（已清空原有权限）: roleCode={}, type={}, count={}", roleCode, permissionType, resourceIds.size());
        } else {
            log.info("清空角色的指定类型数据权限: roleCode={}, type={}", roleCode, permissionType);
        }

        // 更新缓存版本号，使旧缓存失效
        cacheManager.updateCacheVersion();
    }

    @Override
    @Transactional
    public void removePermission(String roleCode, String permissionType, Long resourceId) {
        baseMapper.delete(
            new LambdaQueryWrapper<RoleDataPermission>()
                .eq(RoleDataPermission::getRoleCode, roleCode)
                .eq(RoleDataPermission::getPermissionType, permissionType)
                .eq(RoleDataPermission::getResourceId, resourceId)
        );
        log.info("移除角色数据权限: roleCode={}, type={}, resourceId={}", roleCode, permissionType, resourceId);
        // 更新缓存版本号，使旧缓存失效
        cacheManager.updateCacheVersion();
    }

    @Override
    @Transactional
    public void clearAllPermission(String roleCode) {
        baseMapper.delete(
            new LambdaQueryWrapper<RoleDataPermission>()
                .eq(RoleDataPermission::getRoleCode, roleCode)
        );
        log.info("清除角色所有数据权限: roleCode={}", roleCode);
        // 更新缓存版本号，使旧缓存失效
        cacheManager.updateCacheVersion();
    }

    @Override
    public UserDataScope getDataScopeByRoleCode(String roleCode) {
        // 1. 获取当前版本号
        long version = cacheManager.getCurrentVersion();

        // 2. 构建缓存键
        String cacheKey = getRoleCacheKey(roleCode, version);

        // 3. 尝试从缓存获取
        try {
            Object cachedData = redisUtil.get(cacheKey);
            if (cachedData != null) {
                log.debug("从缓存获取角色权限: roleCode={}, version={}", roleCode, version);
                return (UserDataScope) cachedData;
            }
        } catch (Exception e) {
            log.warn("从缓存获取角色权限失败，继续查询数据库: roleCode={}", roleCode, e);
        }

        // 4. 缓存未命中，执行原有业务逻辑
        UserDataScope dataScope = new UserDataScope();

        // 查询角色权限
        List<RoleDataPermission> permissions = baseMapper.selectList(
            new LambdaQueryWrapper<RoleDataPermission>()
                .eq(RoleDataPermission::getRoleCode, roleCode)
        );

        fillDataScope(dataScope, permissions);
        log.debug("角色权限查询完成: roleCode={}, {}", roleCode, dataScope);

        // 5. 回填缓存
        try {
            redisUtil.set(cacheKey, dataScope, CACHE_EXPIRE_SECONDS);
            log.debug("角色权限已缓存: key={}", cacheKey);
        } catch (Exception e) {
            log.warn("缓存角色权限失败: roleCode={}", roleCode, e);
        }

        return dataScope;
    }

    /**
     * 将权限列表按类型动态分组并填充到 UserDataScope
     */
    private void fillDataScope(UserDataScope dataScope, List<RoleDataPermission> permissions) {
        Map<String, Set<Long>> grouped = permissions.stream()
            .collect(Collectors.groupingBy(
                RoleDataPermission::getPermissionType,
                Collectors.mapping(RoleDataPermission::getResourceId, Collectors.toSet())
            ));

        grouped.forEach(dataScope::setPermissionIds);

        // 已有显式 JSON 字段的类型，保证无数据时也有空集
        for (String type : RoleDataPermission.EXPLICIT_TYPES) {
            dataScope.setPermissionIds(type, grouped.getOrDefault(type, new HashSet<>()));
        }
    }

    /**
     * 获取当前登录用户的所有角色编码
     *
     * 从 Shiro 的 Subject 中获取当前登录用户
     * 使用 LoginUser.getRoleCode() 获取角色编码（多个角色用逗号分割）
     *
     * @return 角色编码集合，如果用户未登录或无角色则返回空集合
     */
    private Set<String> getCurrentUserRoles() {
        try {
            // 从 Shiro 的 Subject 中获取当前登录用户
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

            if (sysUser == null) {
                log.debug("获取用户角色失败，用户未登录");
                return Collections.emptySet();
            }

            // 获取角色编码字符串（多个角色用逗号分割）
            String roleCodeStr = sysUser.getRoleCode();

            if (StrUtil.isEmpty(roleCodeStr)) {
                log.debug("当前用户无角色");
                return Collections.emptySet();
            }

            // 将逗号分割的字符串转换为 Set
            String[] roleCodeArray = roleCodeStr.split(",");
            Set<String> roleCodes = new HashSet<>(roleCodeArray.length);

            for (String roleCode : roleCodeArray) {
                String trimmedCode = roleCode.trim();
                if (StrUtil.isNotEmpty(trimmedCode)) {
                    roleCodes.add(trimmedCode);
                }
            }

            log.debug("获取当前用户角色成功: roleCodes={}", roleCodes);
            return roleCodes;

        } catch (Exception e) {
            log.error("获取当前用户角色异常", e);
            return Collections.emptySet();
        }
    }
}
