package org.jeecg.modules.bems.mdm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.bems.mdm.entity.EquipmentCategory;

import java.util.List;
import java.util.Map;

/**
 * @Description: 设备类别
 * @Author: jeecg-boot
 * @Date:   2025-02-20
 * @Version: V1.0
 */
public interface EquipmentCategoryMapper extends BaseMapper<EquipmentCategory> {

	/**
	 * 编辑节点状态
	 * @param id
	 * @param status
	 */
	void updateTreeNodeStatus(@Param("id") Long id,@Param("status") String status);

	/**
	 * 【vue3专用】根据父级ID查询树节点数据
	 *
	 * @param pid
	 * @param query
	 * @return
	 */
	List<SelectTreeModel> queryListByPid(@Param("pid") Long pid, @Param("query") Map<String, String> query);

	/**
	 * 【vue3专用】根据父级ID查询树节点数据
	 * @param pid
	 * @param type
	 * @return
	 */
	List<SelectTreeModel> queryListByTypeAndPid(@Param("pid")Long pid,@Param("type")String type);
	/**
	 * 更新空间全称、父级id
	 * @param oldFullId
	 * @param fullName
	 * @param fullId
	 */
	void updateFullInfo(@Param("oldFullId")String oldFullId,@Param("fullName")String fullName,@Param("fullId")String fullId);

}
