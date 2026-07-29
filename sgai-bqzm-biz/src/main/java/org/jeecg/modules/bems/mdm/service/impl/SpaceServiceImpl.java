package org.jeecg.modules.bems.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.bems.mdm.entity.Space;
import org.jeecg.modules.bems.mdm.mapper.SpaceMapper;
import org.jeecg.modules.bems.mdm.service.ISpaceService;
import org.jeecg.modules.bems.mdm.vo.PermissionSpaceTreeModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 空间位置
 * @Author: jeecg-boot
 * @Date:   2025-02-20
 * @Version: V1.0
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements ISpaceService {

    @Override
    public List<SelectTreeModel> buildTree() {
        Map<String, List<SelectTreeModel>> listMap = list().stream().sorted(Comparator.comparing(Space::getSort))
                .map(Space::convert)
                .collect(Collectors.groupingBy(SelectTreeModel::getParentId, Collectors.toList()));
        listMap.values().forEach(v -> {
            for(SelectTreeModel item : v){
                item.setChildren(listMap.getOrDefault(item.getKey(),Collections.emptyList()));
            }
        });
        return listMap.getOrDefault("0",Collections.emptyList());
    }

    /**
     * 获取节点及节点上所有父节点
     *
     * @param spaceIds 节点id
     * @return 节点树
     */
    @Override
    public List<SelectTreeModel> buildTree(Collection<Long> spaceIds) {
        if (spaceIds == null || spaceIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询所有空间节点
        List<Space> allSpaces = list();

        // 构建id->space的映射，方便查找
        Map<Long, Space> spaceMap = allSpaces.stream()
                .collect(Collectors.toMap(Space::getId, s -> s));

        // 收集需要包含的节点：指定节点及其所有父节点
        Set<Long> includedIds = new HashSet<>();
        for (Long spaceId : spaceIds) {
            collectNodeAndAncestors(spaceId, spaceMap, includedIds);
        }

        // 过滤出需要包含的节点并转换为SelectTreeModel
        List<SelectTreeModel> filteredModels = allSpaces.stream()
                .filter(s -> includedIds.contains(s.getId()))
                .sorted(Comparator.comparing(Space::getSort))
                .map(Space::convert)
                .collect(Collectors.toList());

        // 按parentId分组
        Map<String, List<SelectTreeModel>> listMap = filteredModels.stream()
                .collect(Collectors.groupingBy(SelectTreeModel::getParentId, Collectors.toList()));

        // 设置children
        listMap.values().forEach(v -> {
            for (SelectTreeModel item : v) {
                item.setChildren(listMap.getOrDefault(item.getKey(), Collections.emptyList()));
            }
        });

        // 返回根节点列表
        return listMap.getOrDefault("0", Collections.emptyList());
    }

    /**
     * 非递归收集节点及其所有祖先节点
     *
     * @param nodeId 起始节点id
     * @param spaceMap 所有节点的映射
     * @param includedIds 收集的结果集
     */
    private void collectNodeAndAncestors(Long nodeId, Map<Long, Space> spaceMap, Set<Long> includedIds) {
        Long currentId = nodeId;
        while (currentId != null && !ISpaceService.ROOT_PID_VALUE.equals(currentId)) {
            if (includedIds.contains(currentId)) {
                // 如果已经包含该节点，说明其祖先节点也已包含，可以提前终止
                break;
            }
            includedIds.add(currentId);
            Space current = spaceMap.get(currentId);
            if (current == null) {
                break;
            }
            currentId = current.getPid();
        }
    }

    @Override
	public void addSpace(Space space) {
        String fullName = space.getSpaceName();
        String fullId = "";
	   //新增时设置hasChild为0
	    space.setHasChild(ISpaceService.NOCHILD);
        // 校验同级别下空间名称是否重复
        if(baseMapper.selectCount(new QueryWrapper<Space>().eq("pid", space.getPid() == null ? ISpaceService.ROOT_PID_VALUE : space.getPid()).eq("space_name", space.getSpaceName())) > 0){
            throw new JeecgBootException("已存在相同名称的空间");
        }
		if(oConvertUtils.isEmpty(space.getPid())){
			space.setPid(ISpaceService.ROOT_PID_VALUE);
            fullId = space.getPid().toString();
		}else{
			//如果当前节点父ID不为空 则设置父节点的hasChildren 为1
			Space parent = baseMapper.selectById(space.getPid());
			if(parent!=null && !"1".equals(parent.getHasChild())){
				parent.setHasChild("1");
				baseMapper.updateById(parent);
			}
            if(parent != null){
                fullName = parent.getFullName() + fullName;
                fullId = parent.getFullId() + ISpaceService.connector + space.getPid();
            }
		}
        space.setFullName(fullName);
        space.setFullId(fullId);
		baseMapper.insert(space);
	}
	
	@Override
	public void updateSpace(Space space) {
		Space entity = this.getById(space.getId());
		if(entity==null) {
			throw new JeecgBootException("未找到对应实体");
		}
        // 校验同级别下名称是否重复
        if(baseMapper.selectCount(new QueryWrapper<Space>().ne("id", space.getId()).eq("pid", space.getPid()).eq("space_name", space.getSpaceName())) > 0){
            throw new JeecgBootException("已存在相同名称的空间");
        }
		Long old_pid = entity.getPid();
		Long new_pid = space.getPid();
		if(!old_pid.equals(new_pid)) {
			updateOldParentNode(old_pid);
			if(oConvertUtils.isEmpty(new_pid)){
				space.setPid(ISpaceService.ROOT_PID_VALUE);
			}
			if(!ISpaceService.ROOT_PID_VALUE.equals(space.getPid())) {
				baseMapper.updateTreeNodeStatus(space.getPid(), ISpaceService.HASCHILD);
			}
		}
        Space parent = null;
        if(space.getPid().compareTo(ISpaceService.ROOT_PID_VALUE) != 0){
            parent = this.getById(space.getPid());
        }
        space.setFullName(parent == null ? space.getSpaceName() : parent.getFullName() + space.getSpaceName());
        space.setFullId(parent == null ? space.getPid().toString() : parent.getFullId() + ISpaceService.connector + space.getPid());
        // 更新子节点fullName、fullId
        baseMapper.updateFullInfo(entity.getFullId() + ISpaceService.connector + entity.getId(),
                space.getFullName(),space.getFullId() + ISpaceService.connector + space.getId());

		baseMapper.updateById(space);
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteSpace(String id) throws JeecgBootException {
		//查询选中节点下所有子节点一并删除
        id = this.queryTreeChildIds(id);
        if(id.indexOf(",")>0) {
            StringBuffer sb = new StringBuffer();
            String[] idArr = id.split(",");
            for (String idVal : idArr) {
                if(idVal != null){
                    Space space = this.getById(idVal);
                    Long pidVal = space.getPid();
                    //查询此节点上一级是否还有其他子节点
                    List<Space> dataList = baseMapper.selectList(new QueryWrapper<Space>().eq("pid", pidVal).notIn("id",Arrays.asList(idArr)));
                    boolean flag = (dataList == null || dataList.isEmpty()) && !Arrays.asList(idArr).contains(pidVal.toString()) && !sb.toString().contains(pidVal.toString());
                    if(flag){
                        //如果当前节点原本有子节点 现在木有了，更新状态
                        sb.append(pidVal).append(",");
                    }
                }
            }
            //批量删除节点
            baseMapper.deleteBatchIds(Arrays.asList(idArr));
            //修改已无子节点的标识
            String[] pidArr = sb.toString().split(",");
            for(String pid : pidArr){
                this.updateOldParentNode(Long.valueOf(pid));
            }
        }else{
            Space space = this.getById(id);
            if(space==null) {
                throw new JeecgBootException("未找到对应实体");
            }
            updateOldParentNode(space.getPid());
            baseMapper.deleteById(id);
        }
	}
	
	@Override
    public List<Space> queryTreeListNoPage(QueryWrapper<Space> queryWrapper) {
        List<Space> dataList = baseMapper.selectList(queryWrapper);
        List<Space> mapList = new ArrayList<>();
        for(Space data : dataList){
            Long pidVal = data.getPid();
            //递归查询子节点的根节点
            if(pidVal != null && !ISpaceService.NOCHILD.equals(pidVal.toString())){
                Space rootVal = this.getTreeRoot(pidVal);
                if(rootVal != null && !mapList.contains(rootVal)){
                    mapList.add(rootVal);
                }
            }else{
                if(!mapList.contains(data)){
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
            LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Space::getPid, parentCode);
            List<Space> list = baseMapper.selectList(queryWrapper);
            if (list == null || list.isEmpty()) {
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
	 * @param pid
	 */
	private void updateOldParentNode(Long pid) {
		if(ISpaceService.ROOT_PID_VALUE.compareTo(pid) != 0) {
			Long count = baseMapper.selectCount(new QueryWrapper<Space>().eq("pid", pid));
			if(count==null || count<=1) {
				baseMapper.updateTreeNodeStatus(pid, ISpaceService.NOCHILD);
			}
		}
	}

	/**
     * 递归查询节点的根节点
     * @param pidVal
     * @return
     */
    private Space getTreeRoot(Long pidVal){
        Space data =  baseMapper.selectById(pidVal);
        if(data != null && ISpaceService.ROOT_PID_VALUE.compareTo(data.getPid()) != 0){
            return this.getTreeRoot(data.getPid());
        }else{
            return data;
        }
    }

    /**
     * 根据id查询所有子节点id
     * @param ids
     * @return
     */
    private String queryTreeChildIds(String ids) {
        //获取id数组
        String[] idArr = ids.split(",");
        StringBuffer sb = new StringBuffer();
        for (String pidVal : idArr) {
            if(pidVal != null){
                if(!sb.toString().contains(pidVal)){
                    if(!sb.toString().isEmpty()){
                        sb.append(",");
                    }
                    sb.append(pidVal);
                    this.getTreeChildIds(Long.valueOf(pidVal),sb);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 递归查询所有子节点
     * @param pidVal
     * @param sb
     * @return
     */
    private StringBuffer getTreeChildIds(Long pidVal,StringBuffer sb){
        List<Space> dataList = baseMapper.selectList(new QueryWrapper<Space>().eq("pid", pidVal));
        if(dataList != null && !dataList.isEmpty()){
            for(Space tree : dataList) {
                if(!sb.toString().contains(tree.getId().toString())){
                    sb.append(",").append(tree.getId());
                }
                this.getTreeChildIds(tree.getId(),sb);
            }
        }
        return sb;
    }

    /**
     * 根据用户权限构建空间树（完全非递归实现）
     *
     * @param spaceIds 用户有权限的空间ID集合
     * @return 包含权限标记的空间树
     */
    @Override
    public List<PermissionSpaceTreeModel> buildPermissionTree(Collection<Long> spaceIds) {
        if (spaceIds == null || spaceIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 查询所有空间节点
        List<Space> allSpaces = list();

        // 2. 构建ID->Space的映射
        Map<Long, Space> spaceMap = allSpaces.stream()
                .collect(Collectors.toMap(Space::getId, s -> s));

        // 3. 收集需要包含的节点：权限节点及其所有父节点（非递归）
        Set<Long> permissionIds = new HashSet<>(spaceIds);
        Set<Long> includedIds = new HashSet<>();
        for (Long spaceId : spaceIds) {
            collectNodeAndAncestors(spaceId, spaceMap, includedIds);
        }

        // 4. 过滤并转换为 PermissionSpaceTreeModel，同时标记权限
        List<PermissionSpaceTreeModel> filteredModels = allSpaces.stream()
                .filter(s -> includedIds.contains(s.getId()))
                .sorted(Comparator.comparing(Space::getSort))
                .map(space -> {
                    PermissionSpaceTreeModel model = new PermissionSpaceTreeModel();
                    model.setKey(space.getId().toString());
                    model.setTitle(space.getSpaceName());
                    model.setParentId(space.getPid().toString());
                    model.setValue(space.getFullName());
                    // 标记复选框状态：无权限时禁用
                    model.setDisableCheckbox(!permissionIds.contains(space.getId()));  // 取反
                    return model;
                })
                .collect(Collectors.toList());

        // 5. 按parentId分组
        Map<String, List<PermissionSpaceTreeModel>> listMap = filteredModels.stream()
                .collect(Collectors.groupingBy(PermissionSpaceTreeModel::getParentId, Collectors.toList()));

        // 6. 设置children（非递归）
        listMap.values().forEach(children -> {
            for (PermissionSpaceTreeModel item : children) {
                @SuppressWarnings("unchecked")
                List<SelectTreeModel> itemChildren = (List<SelectTreeModel>) (List<?>) listMap.getOrDefault(item.getKey(), Collections.emptyList());
                item.setChildren(itemChildren);
            }
        });

        // 7. 返回根节点列表
        return listMap.getOrDefault("0", Collections.emptyList());
    }

}
