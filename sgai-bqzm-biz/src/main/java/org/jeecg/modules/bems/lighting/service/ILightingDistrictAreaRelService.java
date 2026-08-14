package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingDistrictAreaRel;
import org.jeecg.modules.bems.lighting.entity.LightingDistrictGroupVo;

import java.util.List;

/**
 * 照明片区-分组-区域关联服务
 */
public interface ILightingDistrictAreaRelService extends IService<LightingDistrictAreaRel> {

    /**
     * 按片区查询分组-区域树（按分组聚合，分组内区域按 sort 升序）
     *
     * @param districtId 片区ID
     * @return 分组树，无数据返回空列表
     */
    List<LightingDistrictGroupVo> listByDistrict(Long districtId);

    /**
     * 新增分组：把一个片区的多个区域一次性挂到同一分组下（重复的自动跳过）
     *
     * @param districtId 片区ID
     * @param groupName  分组名称
     * @param areaIds    区域ID集合
     * @param remark     备注（可为空）
     */
    void addGroup(Long districtId, String groupName, List<Long> areaIds, String remark);

    /**
     * 删除分组：按片区+分组名删除该分组下的所有关联
     *
     * @param districtId 片区ID
     * @param groupName  分组名称
     */
    void deleteGroup(Long districtId, String groupName);

    /**
     * 分组重命名：把片区下某分组改名为新名称（分组内所有关联一起改）
     *
     * @param districtId 片区ID
     * @param oldName    原分组名称
     * @param newName    新分组名称
     */
    void renameGroup(Long districtId, String oldName, String newName);
}
