package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.dto.MeasureRuleDto;
import org.jeecg.modules.bems.energyAnalysis.dto.MeteringPointDto;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointTreeVo;
import org.jeecg.modules.bems.energyAnalysis.vo.PermissionMeteringPointTreeModel;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointVo;

import java.util.List;

public interface IMeteringPointService extends IService<MeteringPoint> {

    void deleteById(Long id);

    MeteringPoint getById(Long id);

    List<MeteringPointTreeVo> getTree(String type);

    /**
     * 构建计量点权限树（根据用户数据权限过滤）
     * @param type 计量点类型（可选）
     * @return 包含权限标记的计量点树
     */
    List<PermissionMeteringPointTreeModel> getPermissionTree(String type);

    List<MeteringPoint> listByType(String type);

    List<MeteringPoint> getByIds(List<Long> ids);

    IPage<MeteringPoint> listPage(MeasureRuleDto params);

    /**
     * 解析公式
     */
    String analyticFormula(String formula);

    /**
     * 保存公式
     */
    boolean saveFormula(MeteringPoint rule);

    IPage<MeteringPointVo> listPoint(MeteringPointDto params);

    List<MeteringPoint> listByParentId(Long parentId);

    /**
     * 获取总用电规则点（运行拓扑下设备类别为电的一级节点）
     */
    List<MeteringPoint> getTotalElectricityConsumption();

    /**
     * 获取空间拓扑下设备类别为电的二级节点
     */
    List<MeteringPoint> getElectricitySecondaryNodeForSpace();

    /**
     * 获取运行拓扑下设备类别为电的二级节点
     */
    List<MeteringPoint> getElectricitySecondaryNodeForRun();

    /**
     * 获取专业拓扑下设备类别为电的二级节点
     */
    List<MeteringPoint> getElectricitySecondaryForSpecialty();

    /**
     * 获取专业拓扑下设备类别为电的节点
     */
    List<MeteringPoint> getElectricityForSpecialty();

    /**
     * 获取节点及节点下所有子节点
     */
    List<MeteringPoint> getTreeListById(Long id);

    /**
     * 获取节点及节点下指定层级的子节点
     * 例：id=1,level=3,返回id=1的节点以及id=1下两级的节点
     */
    List<MeteringPoint> getTreeListByIdAndLevel(Long id,Integer level);

    /**
     * 获取所有节点树
     */
    List<MeteringPointTreeVo> getAllTree();

    /**
     * 获取点位全称
     * @param id 计量规则点位id
     * @return 全称，例：专业拓扑-照明插座用点-公共照明用点
     */
    String getMeteringPointFullNameById(Long id);

    /**
     *
     * @param entity 实体对象
     * @return
     */
    boolean updateById(MeteringPoint entity);
}
