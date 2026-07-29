package org.jeecg.modules.bems.mdm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.bems.mdm.entity.EquipmentCategory;
import org.jeecg.modules.bems.mdm.vo.PermissionEquipmentCategoryTreeModel;

import java.util.Collection;
import java.util.List;

/**
 * @Description: 设备类别
 * @Author: jeecg-boot
 * @Date:   2025-02-20
 * @Version: V1.0
 */
public interface IEquipmentCategoryService extends IService<EquipmentCategory> {

	/**根节点父ID的值*/
	public static final Long ROOT_PID_VALUE = 0L;

	/** fullId 连接符 */
	public static final String connector = "-";

	/**树节点有子节点状态值*/
	public static final String HASCHILD = "1";
	
	/**树节点无子节点状态值*/
	public static final String NOCHILD = "0";

	/**
	 * 新增节点
	 *
	 * @param equipmentCategory
	 */
	void addEquipmentCategory(EquipmentCategory equipmentCategory);
	
	/**
   * 修改节点
   *
   * @param equipmentCategory
   * @throws JeecgBootException
   */
	void updateEquipmentCategory(EquipmentCategory equipmentCategory) throws JeecgBootException;
	
	/**
	 * 删除节点
	 *
	 * @param id
   * @throws JeecgBootException
	 */
	void deleteEquipmentCategory(String id) throws JeecgBootException;

	  /**
	   * 查询所有数据，无分页
	   *
	   * @param queryWrapper
	   * @return List<EquipmentCategory>
	   */
    List<EquipmentCategory> queryTreeListNoPage(QueryWrapper<EquipmentCategory> queryWrapper);

	/**
	 * 【vue3专用】根据父级编码加载分类字典的数据
	 *
	 * @param parentCode
	 * @return
	 */
	List<SelectTreeModel> queryListByCode(String parentCode);

	List<SelectTreeModel> queryListByTypeAndCode(String type,String parentCode);

	/**
	 * 【vue3专用】根据pid查询子节点集合
	 *
	 * @param pid
	 * @return
	 */
	List<SelectTreeModel> queryListByPid(Long pid);

	/**
	 * 构建树
	 * @return
	 */
	List<SelectTreeModel> buildTree();

	/**
	 * 构建树
	 * @param type 类型。仪表：1；设备：2
	 * @return
	 */
	List<SelectTreeModel> buildTree(String type);

	/**
	 * 根据类型查询列表
	 * @param type 类型。仪表：1；设备：2
	 * @return
	 */
	List<EquipmentCategory> queryListByType(String type);

	List<EquipmentCategory> findByIds(Collection<Long> ids);

	/**
	 * 根据用户权限构建设备类别树（完全非递归实现）
	 *
	 * @param categoryIds 用户有权限的设备类别ID集合
	 * @param type 类型。仪表：1；设备：2；null 表示全部
	 * @return 包含权限标记的设备类别树
	 */
	List<PermissionEquipmentCategoryTreeModel> buildPermissionTree(Collection<Long> categoryIds, String type);

}
