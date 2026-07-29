package org.jeecg.modules.bems.permission.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;
import org.jeecg.modules.bems.permission.vo.UserDataScope;

import java.util.Set;

/**
 * 角色数据权限服务接口
 */
public interface RoleDataPermissionService extends IService<RoleDataPermission> {

    /**
     * 获取当前登录用户的数据权限范围
     * @return 当前登录用户的数据权限范围
     */
    UserDataScope getCurrentUserDataScope();

    /**
     * 为角色分配单个权限
     * @param roleCode 角色编码
     * @param permissionType 权限类型（CATEGORY-专业, SPACE-空间）
     * @param resourceId 资源ID
     */
    void assignPermission(String roleCode, String permissionType, Long resourceId);

    /**
     * 批量为角色分配权限
     * @param roleCode 角色编码
     * @param permissionType 权限类型（CATEGORY-专业, SPACE-空间）
     * @param resourceIds 资源ID集合
     */
    void batchAssignPermission(String roleCode, String permissionType, Set<Long> resourceIds);

    /**
     * 移除角色权限
     * @param roleCode 角色编码
     * @param permissionType 权限类型
     * @param resourceId 资源ID
     */
    void removePermission(String roleCode, String permissionType, Long resourceId);

    /**
     * 清除角色所有权限
     * @param roleCode 角色编码
     */
    void clearAllPermission(String roleCode);

    /**
     * 根据角色编码获取数据权限范围
     * @param roleCode 角色编码
     * @return 角色数据权限范围
     */
    UserDataScope getDataScopeByRoleCode(String roleCode);
}
