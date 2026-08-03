package org.jeecg.modules.bems.permission.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.bems.permission.dto.BatchAssignPermissionDto;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;
import org.jeecg.modules.bems.permission.service.RoleDataPermissionService;
import org.jeecg.modules.bems.permission.vo.UserDataScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据权限管理接口
 * 提供角色数据权限的查询、分配、移除等功能
 */
@Api(tags="数据权限管理")
@RestController
@RequestMapping("/bems/dataPermission")
@Slf4j
public class DataPermissionController {

    @Autowired
    private RoleDataPermissionService permissionService;


    @ApiOperation(value="查询当前用户数据权限", notes="查询当前用户的数据权限范围")
    @GetMapping("/current")
    public Result<UserDataScope> getCurrentUserPermission() {
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (sysUser == null) {
            return Result.error("未登录用户");
        }
        UserDataScope dataScope = permissionService.getCurrentUserDataScope();
        return Result.ok(dataScope);
    }

    @ApiOperation(value="查询角色数据权限", notes="根据角色编码查询角色的数据权限范围")
    @GetMapping("/role/{roleCode}")
    public Result<UserDataScope> getRolePermission(@PathVariable String roleCode) {
        try {
            UserDataScope dataScope = permissionService.getDataScopeByRoleCode(roleCode);
            return Result.ok(dataScope);
        } catch (Exception e) {
            log.error("查询角色权限失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @ApiOperation(value="为角色分配专业权限", notes="为角色分配单个专业权限")
    @PostMapping("/assign/category")
    @AutoLog(value = "分配专业权限")
    public Result<String> assignCategoryPermission(
            @RequestParam String roleCode,
            @RequestParam Long categoryId) {
        try {
            permissionService.assignPermission(roleCode, RoleDataPermission.TYPE_CATEGORY, categoryId);
            return Result.ok("专业权限分配成功");
        } catch (Exception e) {
            log.error("分配专业权限失败", e);
            return Result.error("分配失败：" + e.getMessage());
        }
    }

    @ApiOperation(value="批量分配专业权限", notes="批量为角色分配多个专业权限，ids为空时清除该角色的所有专业权限")
    @PostMapping("/assign/category/batch")
    @AutoLog(value = "批量分配专业权限")
    public Result<String> batchAssignCategoryPermission(
            @RequestBody BatchAssignPermissionDto dto) {
        try {
            permissionService.batchAssignPermission(dto.getRoleCode(), RoleDataPermission.TYPE_CATEGORY, dto.getIds());
            if (dto.getIds() == null || dto.getIds().isEmpty()) {
                return Result.ok("专业权限已清除");
            }
            return Result.ok("批量分配专业权限成功，共" + dto.getIds().size() + "个");
        } catch (Exception e) {
            log.error("批量分配专业权限失败", e);
            return Result.error("批量分配失败：" + e.getMessage());
        }
    }

    @ApiOperation(value="为角色分配空间权限", notes="为角色分配单个空间权限")
    @PostMapping("/assign/space")
    @AutoLog(value = "分配空间权限")
    public Result<String> assignSpacePermission(
            @RequestParam String roleCode,
            @RequestParam Long spaceId) {
        try {
            permissionService.assignPermission(roleCode, RoleDataPermission.TYPE_SPACE, spaceId);
            return Result.ok("空间权限分配成功");
        } catch (Exception e) {
            log.error("分配空间权限失败", e);
            return Result.error("分配失败：" + e.getMessage());
        }
    }

    @ApiOperation(value="批量分配空间权限", notes="批量为角色分配多个空间权限，ids为空时清除该角色的所有空间权限")
    @PostMapping("/assign/space/batch")
    @AutoLog(value = "批量分配空间权限")
    public Result<String> batchAssignSpacePermission(
            @RequestBody BatchAssignPermissionDto dto) {
        try {
            permissionService.batchAssignPermission(dto.getRoleCode(), RoleDataPermission.TYPE_SPACE, dto.getIds());
            if (dto.getIds() == null || dto.getIds().isEmpty()) {
                return Result.ok("空间权限已清除");
            }
            return Result.ok("批量分配空间权限成功，共" + dto.getIds().size() + "个");
        } catch (Exception e) {
            log.error("批量分配空间权限失败", e);
            return Result.error("批量分配失败：" + e.getMessage());
        }
    }

    @ApiOperation(value="为角色分配照明权限", notes="为角色分配单个照明权限")
    @PostMapping("/assign/lighting")
    @AutoLog(value = "分配照明权限")
    public Result<String> assignLightingPermission(
            @RequestParam String roleCode,
            @RequestParam Long lightingId) {
        try {
            permissionService.assignPermission(roleCode, RoleDataPermission.TYPE_LIGHTING, lightingId);
            return Result.ok("照明权限分配成功");
        } catch (Exception e) {
            log.error("分配照明权限失败", e);
            return Result.error("分配失败：" + e.getMessage());
        }
    }

    @ApiOperation(value="批量分配照明权限", notes="批量为角色分配多个照明权限，ids为空时清除该角色的所有照明权限")
    @PostMapping("/assign/lighting/batch")
    @AutoLog(value = "批量分配照明权限")
    public Result<String> batchAssignLightingPermission(
            @RequestBody BatchAssignPermissionDto dto) {
        try {
            permissionService.batchAssignPermission(dto.getRoleCode(), RoleDataPermission.TYPE_LIGHTING, dto.getIds());
            if (dto.getIds() == null || dto.getIds().isEmpty()) {
                return Result.ok("照明权限已清除");
            }
            return Result.ok("批量分配照明权限成功，共" + dto.getIds().size() + "个");
        } catch (Exception e) {
            log.error("批量分配照明权限失败", e);
            return Result.error("批量分配失败：" + e.getMessage());
        }
    }

    @ApiOperation(value="移除角色数据权限", notes="移除角色的指定权限")
    @DeleteMapping("/remove")
    @AutoLog(value = "移除数据权限")
    public Result<String> removePermission(
            @RequestParam String roleCode,
            @RequestParam String permissionType,
            @RequestParam Long resourceId) {
        try {
            permissionService.removePermission(roleCode, permissionType, resourceId);
            return Result.ok("权限移除成功");
        } catch (Exception e) {
            log.error("移除权限失败", e);
            return Result.error("移除失败：" + e.getMessage());
        }
    }

    @ApiOperation(value="清除角色所有数据权限", notes="清除指定角色的所有数据权限")
    @DeleteMapping("/clear")
    @AutoLog(value = "清除所有数据权限")
    public Result<String> clearAllPermission(@RequestParam String roleCode) {
        try {
            permissionService.clearAllPermission(roleCode);
            return Result.ok("所有权限已清除");
        } catch (Exception e) {
            log.error("清除权限失败", e);
            return Result.error("清除失败：" + e.getMessage());
        }
    }

}
