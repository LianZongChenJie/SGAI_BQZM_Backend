package org.jeecg.modules.bems.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.BaseEntity;

import java.util.Set;

/**
 * 角色数据权限实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role_data_permission")
@ApiModel(value="角色数据权限对象", description="角色数据权限")
public class RoleDataPermission extends BaseEntity {

    public static final String TYPE_CATEGORY = "CATEGORY";
    public static final String TYPE_SPACE = "SPACE";
    public static final String TYPE_LIGHTING = "LIGHTING";
    public static final String TYPE_DISTRICT = "DISTRICT";
    public static final String TYPE_TAG = "TAG";

    /** 已有显式 JSON 字段的权限类型，新增类型时在此处追加 */
    public static final Set<String> EXPLICIT_TYPES = Set.of(TYPE_CATEGORY, TYPE_SPACE, TYPE_LIGHTING, TYPE_DISTRICT, TYPE_TAG);

    @ApiModelProperty(value = "角色编码")
    private String roleCode;

    @ApiModelProperty(value = "权限类型：CATEGORY-专业, SPACE-空间, LIGHTING-照明")
    private String permissionType;

    @ApiModelProperty(value = "资源ID")
    private Long resourceId;
}
