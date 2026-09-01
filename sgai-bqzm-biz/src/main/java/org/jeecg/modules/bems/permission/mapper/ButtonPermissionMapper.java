package org.jeecg.modules.bems.permission.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 按钮权限码查询 Mapper
 * 查询 jeecg 系统权限表（sys_permission）中，指定用户拥有（通过角色关联）的权限码集合。
 *
 * 表位于 JEECG-BOOT schema（系统表），业务库为 BQZM，因此查询需显式带 schema 前缀。
 */
public interface ButtonPermissionMapper {

    /**
     * 查询指定用户拥有的权限码（perms）集合
     * 用户 -> sys_user_role(角色) -> sys_role_permission(权限) -> sys_permission(perms)
     *
     * @param userId 用户ID
     * @return 权限码集合
     */
    @Select("SELECT DISTINCT p.perms FROM \"JEECG-BOOT\".\"sys_role_permission\" rp " +
            "JOIN \"JEECG-BOOT\".\"sys_permission\" p ON p.id = rp.permission_id " +
            "JOIN \"JEECG-BOOT\".\"sys_user_role\" ur ON ur.role_id = rp.role_id " +
            "WHERE ur.user_id = #{userId} AND p.perms IS NOT NULL AND p.perms <> ''")
    List<String> selectPermsByUserId(@Param("userId") String userId);
}
