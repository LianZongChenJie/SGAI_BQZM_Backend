package org.jeecg.modules.bems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.bems.entity.EnergyMediumManage;
import org.jeecg.modules.bems.mapper.EnergyMediumManageMapper;
import org.jeecg.modules.bems.service.IEnergyMediumManageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Description: 能介管理
 * @Author: jeecg-boot
 * @Date: 2025-02-25
 * @Version: V1.0
 */
@Service
public class EnergyMediumManageServiceImpl extends ServiceImpl<EnergyMediumManageMapper, EnergyMediumManage> implements IEnergyMediumManageService {

    @Override
    public void addEnergyMediumManage(EnergyMediumManage energyMediumManage) {
        //新增时设置hasChild为0
        energyMediumManage.setHasChild(IEnergyMediumManageService.NOCHILD);
        // 校验能介编码、能介名称是否重复
        if (baseMapper.selectCount(new QueryWrapper<EnergyMediumManage>().eq("code", energyMediumManage.getCode())) > 0) {
            throw new JeecgBootException("已存在相同编码的类别");
        }
        if (baseMapper.selectCount(new QueryWrapper<EnergyMediumManage>().eq("name", energyMediumManage.getName())) > 0) {
            throw new JeecgBootException("已存在相同名称的类别");
        }
        if (oConvertUtils.isEmpty(energyMediumManage.getPid())) {
            energyMediumManage.setPid(IEnergyMediumManageService.ROOT_PID_VALUE);
        } else {
            //如果当前节点父ID不为空 则设置父节点的hasChildren 为1
            EnergyMediumManage parent = baseMapper.selectById(energyMediumManage.getPid());
            if (parent != null && !"1".equals(parent.getHasChild())) {
                parent.setHasChild("1");
                baseMapper.updateById(parent);
            }
        }
        baseMapper.insert(energyMediumManage);
    }

    @Override
    public void updateEnergyMediumManage(EnergyMediumManage energyMediumManage) {
        EnergyMediumManage entity = this.getById(energyMediumManage.getId());
        if (entity == null) {
            throw new JeecgBootException("未找到对应实体");
        }
        // 校验能介编码、能介名称是否重复
        if (baseMapper.selectCount(new QueryWrapper<EnergyMediumManage>().eq("code", energyMediumManage.getCode()).ne("id", energyMediumManage.getId())) > 0) {
            throw new JeecgBootException("已存在相同编码的类别");
        }
        if (baseMapper.selectCount(new QueryWrapper<EnergyMediumManage>().eq("name", energyMediumManage.getName()).ne("id", energyMediumManage.getId())) > 0) {
            throw new JeecgBootException("已存在相同名称的类别");
        }
        Long old_pid = entity.getPid();
        Long new_pid = energyMediumManage.getPid();
        if (!old_pid.equals(new_pid)) {
            updateOldParentNode(old_pid);
            if (oConvertUtils.isEmpty(new_pid)) {
                energyMediumManage.setPid(IEnergyMediumManageService.ROOT_PID_VALUE);
            }
            if (!IEnergyMediumManageService.ROOT_PID_VALUE.equals(energyMediumManage.getPid())) {
                baseMapper.updateTreeNodeStatus(energyMediumManage.getPid(), IEnergyMediumManageService.HASCHILD);
            }
        }
        baseMapper.updateById(energyMediumManage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEnergyMediumManage(String id) throws JeecgBootException {
        //查询选中节点下所有子节点一并删除
        id = this.queryTreeChildIds(id);
        if (id.indexOf(",") > 0) {
            StringBuffer sb = new StringBuffer();
            String[] idArr = id.split(",");
            for (String idVal : idArr) {
                if (idVal != null) {
                    EnergyMediumManage energyMediumManage = this.getById(idVal);
                    Long pidVal = energyMediumManage.getPid();
                    //查询此节点上一级是否还有其他子节点
                    List<EnergyMediumManage> dataList = baseMapper.selectList(new QueryWrapper<EnergyMediumManage>().eq("pid", pidVal).notIn("id", Arrays.asList(idArr)));
                    boolean flag = (dataList == null || dataList.size() == 0) && !Arrays.asList(idArr).contains(pidVal) && !sb.toString().contains(pidVal.toString());
                    if (flag) {
                        //如果当前节点原本有子节点 现在木有了，更新状态
                        sb.append(pidVal).append(",");
                    }
                }
            }
            //批量删除节点
            baseMapper.deleteBatchIds(Arrays.asList(idArr));
            //修改已无子节点的标识
            String[] pidArr = sb.toString().split(",");
            for (String pid : pidArr) {
                this.updateOldParentNode(Long.valueOf(pid));
            }
        } else {
            EnergyMediumManage energyMediumManage = this.getById(id);
            if (energyMediumManage == null) {
                throw new JeecgBootException("未找到对应实体");
            }
            updateOldParentNode(energyMediumManage.getPid());
            baseMapper.deleteById(id);
        }
    }

    @Override
    public List<EnergyMediumManage> queryTreeListNoPage(QueryWrapper<EnergyMediumManage> queryWrapper) {
        List<EnergyMediumManage> dataList = baseMapper.selectList(queryWrapper);
        List<EnergyMediumManage> mapList = new ArrayList<>();
        for (EnergyMediumManage data : dataList) {
            Long pidVal = data.getPid();
            //递归查询子节点的根节点
            if (pidVal != null && !IEnergyMediumManageService.NOCHILD.equals(pidVal.toString())) {
                EnergyMediumManage rootVal = this.getTreeRoot(pidVal);
                if (rootVal != null && !mapList.contains(rootVal)) {
                    mapList.add(rootVal);
                }
            } else {
                if (!mapList.contains(data)) {
                    mapList.add(data);
                }
            }
        }
        return mapList;
    }

    @Override
    public List<SelectTreeModel> queryListByCode(String parentCode) {
        Long pid = ROOT_PID_VALUE;
        if (oConvertUtils.isNotEmpty(parentCode)) {
            LambdaQueryWrapper<EnergyMediumManage> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(EnergyMediumManage::getPid, parentCode);
            List<EnergyMediumManage> list = baseMapper.selectList(queryWrapper);
            if (list == null || list.size() == 0) {
                throw new JeecgBootException("该编码【" + parentCode + "】不存在，请核实!");
            }
            if (list.size() > 1) {
                throw new JeecgBootException("该编码【" + parentCode + "】存在多个，请核实!");
            }
            pid = list.get(0).getId();
        }
        return baseMapper.queryListByPid(pid, null);
    }

    @Override
    public List<SelectTreeModel> queryListByPid(Long pid) {
        if (oConvertUtils.isEmpty(pid)) {
            pid = ROOT_PID_VALUE;
        }
        return baseMapper.queryListByPid(pid, null);
    }

    /**
     * 根据所传pid查询旧的父级节点的子节点并修改相应状态值
     *
     * @param pid
     */
    private void updateOldParentNode(Long pid) {
        if (IEnergyMediumManageService.ROOT_PID_VALUE.compareTo(pid) != 0) {
            Long count = baseMapper.selectCount(new QueryWrapper<EnergyMediumManage>().eq("pid", pid));
            if (count == null || count <= 1) {
                baseMapper.updateTreeNodeStatus(pid, IEnergyMediumManageService.NOCHILD);
            }
        }
    }

    /**
     * 递归查询节点的根节点
     *
     * @param pidVal
     * @return
     */
    private EnergyMediumManage getTreeRoot(Long pidVal) {
        EnergyMediumManage data = baseMapper.selectById(pidVal);
        if (data != null && !IEnergyMediumManageService.ROOT_PID_VALUE.equals(data.getPid())) {
            return this.getTreeRoot(data.getPid());
        } else {
            return data;
        }
    }

    /**
     * 根据id查询所有子节点id
     *
     * @param ids
     * @return
     */
    private String queryTreeChildIds(String ids) {
        //获取id数组
        String[] idArr = ids.split(",");
        StringBuffer sb = new StringBuffer();
        for (String pidVal : idArr) {
            if (pidVal != null) {
                if (!sb.toString().contains(pidVal)) {
                    if (sb.toString().length() > 0) {
                        sb.append(",");
                    }
                    sb.append(pidVal);
                    this.getTreeChildIds(pidVal, sb);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 递归查询所有子节点
     *
     * @param pidVal
     * @param sb
     * @return
     */
    private StringBuffer getTreeChildIds(String pidVal, StringBuffer sb) {
        List<EnergyMediumManage> dataList = baseMapper.selectList(new QueryWrapper<EnergyMediumManage>().eq("pid", pidVal));
        if (dataList != null && dataList.size() > 0) {
            for (EnergyMediumManage tree : dataList) {
                if (!sb.toString().contains(tree.getId().toString())) {
                    sb.append(",").append(tree.getId());
                }
                this.getTreeChildIds(tree.getId().toString(), sb);
            }
        }
        return sb;
    }

}
