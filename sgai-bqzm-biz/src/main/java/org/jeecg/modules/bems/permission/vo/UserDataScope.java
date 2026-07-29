package org.jeecg.modules.bems.permission.vo;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.*;

/**
 * 用户数据权限范围
 * 通过 permissionData Map 动态支持所有权限类型
 * JSON 序列化/反序列化由 @JsonAnyGetter / @JsonAnySetter 自动处理
 */
@Data
public class UserDataScope {
    /**
     * 权限数据 Map
     * Key: 权限类型 (如 "CATEGORY", "SPACE", "LIGHTING" 等)
     * Value: 该权限类型下有权限的ID集合
     */
    @JsonIgnore
    private Map<String, Set<Long>> permissionData;

    // ========== 构造函数 ==========

    public UserDataScope() {
        this.permissionData = new HashMap<>();
    }

    // ========== 核心方法 ==========

    /**
     * 获取指定权限类型的ID集合
     */
    public Set<Long> getPermissionIds(String permissionType) {
        return permissionData != null ? permissionData.get(permissionType) : null;
    }

    /**
     * 设置指定权限类型的ID集合
     */
    public void setPermissionIds(String permissionType, Set<Long> ids) {
        if (permissionData == null) {
            permissionData = new HashMap<>();
        }
        permissionData.put(permissionType, ids);
    }

    // ========== JSON 序列化/反序列化 ==========

    /**
     * 将所有权限类型输出为 JSON 字段
     * 例如：{"categoryIds": [1,2], "spaceIds": [3,4], "lightingIds": [5,6]}
     */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        Map<String, Object> result = new HashMap<>();

        if (permissionData != null) {
            for (Map.Entry<String, Set<Long>> entry : permissionData.entrySet()) {
                String fieldName = entry.getKey().toLowerCase() + "Ids";
                result.put(fieldName, entry.getValue());
            }
        }

        return result;
    }

    /**
     * 从 JSON 反序列化权限类型字段
     * 兼容 Collection（List/Set）类型
     */
    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        if (key.endsWith("Ids") && value instanceof Collection) {
            String type = key.substring(0, key.length() - 3).toUpperCase();
            @SuppressWarnings("unchecked")
            Collection<Long> ids = (Collection<Long>) value;
            setPermissionIds(type, new HashSet<>(ids));
        }
    }
}
