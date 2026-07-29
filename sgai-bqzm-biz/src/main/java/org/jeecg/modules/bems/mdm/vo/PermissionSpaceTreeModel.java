package org.jeecg.modules.bems.mdm.vo;

import org.jeecg.common.system.vo.SelectTreeModel;

/**
 * 带权限标记的空间树形模型
 * 扩展自 SelectTreeModel，增加权限范围标记
 *
 * @Description: 空间位置-权限树形模型
 * @Author: jeecg-boot
 * @Date: 2025-03-24
 * @Version: V1.0
 */
public class PermissionSpaceTreeModel extends SelectTreeModel {

    private static final long serialVersionUID = 1L;

    /**
     * 是否禁用复选框
     * true: 此节点仅为父级节点，用户无直接权限，禁用复选框
     * false: 用户有权限访问此节点，启用复选框
     */
    private Boolean disableCheckbox;

    public Boolean getDisableCheckbox() {
        return disableCheckbox;
    }

    public void setDisableCheckbox(Boolean disableCheckbox) {
        this.disableCheckbox = disableCheckbox;
    }
}
