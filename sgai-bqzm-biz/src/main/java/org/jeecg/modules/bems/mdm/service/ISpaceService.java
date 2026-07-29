package org.jeecg.modules.bems.mdm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.bems.mdm.entity.Space;
import org.jeecg.modules.bems.mdm.vo.PermissionSpaceTreeModel;

import java.util.Collection;
import java.util.List;

/**
 * @Description: 空间位置
 * @Author: jeecg-boot
 * @Date:   2025-02-20
 * @Version: V1.0
 */
public interface ISpaceService extends IService<Space> {

	/**根节点父ID的值*/
	public static final Long ROOT_PID_VALUE = 0L;

	/** fullId 连接符 */
	public static final String connector = "-";
	
	/**树节点有子节点状态值*/
	public static final String HASCHILD = "1";
	
	/**树节点无子节点状态值*/
	public static final String NOCHILD = "0";

	List<SelectTreeModel> buildTree();

	/**
	 * 获取节点及节点上所有父节点
	 * @param spaceIds 节点id
	 * @return 节点树
	 */
	List<SelectTreeModel> buildTree(Collection<Long> spaceIds);

	/**
	 * 根据用户权限构建空间树
	 * @param spaceIds 用户有权限的空间ID集合
	 * @return 包含权限标记的空间树
	 */
	List<PermissionSpaceTreeModel> buildPermissionTree(Collection<Long> spaceIds);

	/**
	 * 新增节点
	 *
	 * @param space
	 */
	void addSpace(Space space);
	
	/**
   * 修改节点
   *
   * @param space
   * @throws JeecgBootException
   */
	void updateSpace(Space space) throws JeecgBootException;
	
	/**
	 * 删除节点
	 *
	 * @param id
   * @throws JeecgBootException
	 */
	void deleteSpace(String id) throws JeecgBootException;

	  /**
	   * 查询所有数据，无分页
	   *
	   * @param queryWrapper
	   * @return List<Space>
	   */
    List<Space> queryTreeListNoPage(QueryWrapper<Space> queryWrapper);

	/**
	 * 【vue3专用】根据父级编码加载分类字典的数据
	 *
	 * @param parentCode
	 * @return
	 */
	List<SelectTreeModel> queryListByCode(String parentCode);

	/**
	 * 【vue3专用】根据pid查询子节点集合
	 *
	 * @param pid
	 * @return
	 */
	List<SelectTreeModel> queryListByPid(Long pid);

}
