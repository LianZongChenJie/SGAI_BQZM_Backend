package org.jeecg.modules.bems.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.bems.entity.EnergyMediumManage;

import java.util.List;
import java.util.Map;

/**
 * @Description: 能介管理
 * @Author: jeecg-boot
 * @Date:   2025-02-25
 * @Version: V1.0
 */
public interface EnergyMediumManageMapper extends BaseMapper<EnergyMediumManage> {

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

}
