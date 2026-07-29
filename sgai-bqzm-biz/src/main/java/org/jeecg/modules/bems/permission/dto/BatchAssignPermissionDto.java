package org.jeecg.modules.bems.permission.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Set;

/**
 * 批量分配权限DTO
 */
@Data
@ApiModel(value = "批量分配权限DTO", description = "批量分配数据权限参数")
public class BatchAssignPermissionDto {

    @ApiModelProperty(value = "角色编码", required = true)
    private String roleCode;

    @ApiModelProperty(value = "资源ID集合", required = true)
    private Set<Long> ids;
}
