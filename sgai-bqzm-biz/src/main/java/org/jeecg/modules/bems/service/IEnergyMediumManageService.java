package org.jeecg.modules.bems.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.bems.entity.EnergyMediumManage;

import java.util.List;

/**
 * @Description: 能介管理
 * @Author: jeecg-boot
 * @Date: 2025-02-25
 * @Version: V1.0
 */
public interface IEnergyMediumManageService extends IService<EnergyMediumManage> {

    /**
     * 根节点父ID的值
     */
    public static final Long ROOT_PID_VALUE = 0L;

    /**
     * 树节点有子节点状态值
     */
    public static final String HASCHILD = "1";

    /**
     * 树节点无子节点状态值
     */
    public static final String NOCHILD = "0";

    /**
     * 新增节点
     *
     * @param energyMediumManage
     */
    void addEnergyMediumManage(EnergyMediumManage energyMediumManage);

    /**
     * 修改节点
     *
     * @param energyMediumManage
     * @throws JeecgBootException
     */
    void updateEnergyMediumManage(EnergyMediumManage energyMediumManage) throws JeecgBootException;

    /**
     * 删除节点
     *
     * @param id
     * @throws JeecgBootException
     */
    void deleteEnergyMediumManage(String id) throws JeecgBootException;

    /**
     * 查询所有数据，无分页
     *
     * @param queryWrapper
     * @return List<EnergyMediumManage>
     */
    List<EnergyMediumManage> queryTreeListNoPage(QueryWrapper<EnergyMediumManage> queryWrapper);

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
