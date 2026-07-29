package org.jeecg.modules.bems.energyAnalysis.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.vo.DictModel;
import org.jeecg.modules.bems.energyAnalysis.constant.MeteringPointConstant;
import org.jeecg.modules.bems.energyAnalysis.dto.MeasureRuleDto;
import org.jeecg.modules.bems.energyAnalysis.dto.MeteringPointDto;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointRel;
import org.jeecg.modules.bems.energyAnalysis.mapper.MeteringPointMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointRelService;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointService;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointTreeVo;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointVo;
import org.jeecg.modules.bems.energyAnalysis.vo.PermissionMeteringPointTreeModel;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;
import org.jeecg.modules.bems.permission.service.RoleDataPermissionService;
import org.jeecg.modules.bems.permission.vo.UserDataScope;
import org.jeecg.modules.bems.mdm.constant.DeviceConstant;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MeteringPointServiceImpl extends ServiceImpl<MeteringPointMapper, MeteringPoint> implements IMeteringPointService {

    private final IDeviceService deviceService;

    private final IMeteringPointRelService meteringPointRelService;

    private final ISysBaseAPI sysBaseAPI;

    private final RoleDataPermissionService roleDataPermissionService;

    @Override
    public boolean save(MeteringPoint entity) {
        if (entity.getParentId() == null) {
            entity.setParentId(MeteringPoint.ROOT_ID);
        }
        if (baseMapper.selectCount(new LambdaQueryWrapper<MeteringPoint>().eq(MeteringPoint::getType,entity.getType()).eq(MeteringPoint::getNodeName, entity.getNodeName())) > 0) {
            // 校验名称是否存在
            throw new JeecgBootException("项目名称已存在");
        }
        for(int i = 0; i < 10; i++){
            String nodeCode = generateCode(entity);
            if(baseMapper.selectCount(new LambdaQueryWrapper<MeteringPoint>().eq(MeteringPoint::getNodeCode, nodeCode)) == 0){
                entity.setNodeCode(nodeCode);
                break;
            }
        }
        if(StringUtils.isEmpty(entity.getNodeCode())){
            throw new JeecgBootException("生成节点编码失败,请稍后再试");
        }
        return super.save(entity);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        MeteringPoint point = super.getById(id);
        if(point == null){
            return;
        }
        // 校验是否存在关联关系
        List<MeteringPointRel> relList = meteringPointRelService.findByTypeAndRelId(MeteringPointRel.TYPE_METERING_POINT, id);
        if(CollectionUtil.isNotEmpty(relList)){
            throw new JeecgBootException("该点位存在关联关系,请先解除关联关系");
        }
        // 删除点位及下级点位信息
        String type = point.getType();
        Map<Long,List<MeteringPoint>> meteringPointMap = listByType(type)
                .stream()
                .collect(Collectors.groupingBy(MeteringPoint::getParentId,Collectors.toList()));
        List<Long> ids = new ArrayList<Long>(){{add(id);}};
        for(int i = 0; i < ids.size(); i++){
            Long l = ids.get(i);
            List<MeteringPoint> meteringPoints = meteringPointMap.get(l);
            if(CollectionUtil.isNotEmpty(meteringPoints)){
                ids.addAll(meteringPoints.stream().map(MeteringPoint::getId).collect(Collectors.toList()));
            }
        }
        // 批量删除
        super.removeByIds(ids);
        meteringPointRelService.removeByPointIds(ids);
    }

    @Override
    public MeteringPoint getById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<MeteringPointTreeVo> getTree(String type) {
        Map<Long, List<MeteringPointTreeVo>> listMap = list(new LambdaQueryWrapper<MeteringPoint>().eq(MeteringPoint::getType, type))
                .stream()
                .map(MeteringPointTreeVo::convert)
                .sorted(Comparator.comparing(MeteringPointTreeVo::getSort))
                .collect(Collectors.groupingBy(MeteringPointTreeVo::getParentId, Collectors.toList()));
        listMap.values().forEach(v -> {
            for (MeteringPointTreeVo item : v) {
                item.setChildren(listMap.getOrDefault(item.getId(), new ArrayList<>()));
            }
        });
        return listMap.getOrDefault(MeteringPoint.ROOT_ID, new ArrayList<>());
    }

    @Override
    public List<PermissionMeteringPointTreeModel> getPermissionTree(String type) {
        // 1. 获取当前登录用户的数据权限范围
        UserDataScope dataScope = roleDataPermissionService.getCurrentUserDataScope();
        Set<Long> categoryIds = dataScope.getPermissionIds(RoleDataPermission.TYPE_CATEGORY);
        Set<Long> spaceIds = dataScope.getPermissionIds(RoleDataPermission.TYPE_SPACE);

        // 3. 如果没有任何权限，返回空树
        if ((categoryIds == null || categoryIds.isEmpty()) && (spaceIds == null || spaceIds.isEmpty())) {
            return Collections.emptyList();
        }

        // 4. 查询所有计量点节点（支持按 type 过滤）
        LambdaQueryWrapper<MeteringPoint> wrapper = new LambdaQueryWrapper<MeteringPoint>()
                .eq(StringUtils.isNotEmpty(type), MeteringPoint::getType, type);
        List<MeteringPoint> allPoints = list(wrapper);

        // 5. 构建ID->MeteringPoint的映射
        Map<Long, MeteringPoint> pointMap = allPoints.stream()
                .collect(Collectors.toMap(MeteringPoint::getId, p -> p));

        // 6. 收集有权限的节点ID及其所有父节点ID（非递归）
        Set<Long> permissionPointIds = new HashSet<>();
        for (MeteringPoint point : allPoints) {
            boolean hasPermission = (categoryIds != null && point.getCategoryId() != null && categoryIds.contains(point.getCategoryId()))
                    && (spaceIds != null && point.getSpaceId() != null && spaceIds.contains(point.getSpaceId()));
            if (hasPermission) {
                // 收集有权限的节点及其所有祖先节点
                collectNodeAndAncestors(point.getId(), pointMap, permissionPointIds);
            }
        }

        // 7. 如果没有权限节点，返回空树
        if (permissionPointIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 8. 过滤并转换为 PermissionMeteringPointTreeModel，同时标记权限
        List<PermissionMeteringPointTreeModel> filteredModels = allPoints.stream()
                .filter(p -> permissionPointIds.contains(p.getId()))
                .sorted(Comparator.comparing(MeteringPoint::getSort))
                .map(point -> {
                    PermissionMeteringPointTreeModel model = new PermissionMeteringPointTreeModel();
                    model.setKey(point.getId().toString());
                    model.setTitle(point.getNodeName());
                    model.setParentId(point.getParentId() == null ? "0" : point.getParentId().toString());
                    model.setValue(point.getNodeCode());
                    // 标记复选框状态：无直接权限的节点禁用复选框
                    boolean hasDirectPermission = (categoryIds != null && point.getCategoryId() != null && categoryIds.contains(point.getCategoryId()))
                            && (spaceIds != null && point.getSpaceId() != null && spaceIds.contains(point.getSpaceId()));
                    model.setDisableCheckbox(!hasDirectPermission);  // 取反：无权限时禁用
                    return model;
                })
                .collect(Collectors.toList());

        // 9. 按parentId分组
        Map<String, List<PermissionMeteringPointTreeModel>> listMap = filteredModels.stream()
                .collect(Collectors.groupingBy(PermissionMeteringPointTreeModel::getParentId, Collectors.toList()));

        // 10. 设置children（非递归）
        listMap.values().forEach(children -> {
            for (PermissionMeteringPointTreeModel item : children) {
                @SuppressWarnings("unchecked")
                List<org.jeecg.common.system.vo.SelectTreeModel> itemChildren =
                        (List<org.jeecg.common.system.vo.SelectTreeModel>) (List<?>) listMap.getOrDefault(item.getKey(), Collections.emptyList());
                item.setChildren(itemChildren);
            }
        });

        // 11. 返回根节点列表（parentId=0）
        return listMap.getOrDefault("0", Collections.emptyList());
    }

    /**
     * 收集节点及其所有祖先节点（非递归实现）
     */
    private void collectNodeAndAncestors(Long nodeId, Map<Long, MeteringPoint> pointMap, Set<Long> collectedIds) {
        Long currentId = nodeId;
        while (currentId != null && !currentId.equals(MeteringPoint.ROOT_ID)) {
            if (collectedIds.contains(currentId)) {
                break; // 已处理过，避免重复
            }
            collectedIds.add(currentId);
            MeteringPoint current = pointMap.get(currentId);
            if (current == null) {
                break;
            }
            currentId = current.getParentId();
        }
    }

    @Override
    public List<MeteringPoint> listByType(String type) {
        return list(new LambdaQueryWrapper<MeteringPoint>().eq(MeteringPoint::getType, type));
    }

    @Override
    public List<MeteringPoint> getByIds(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<MeteringPoint>().in(MeteringPoint::getId, ids));
    }

    @Override
    public IPage<MeteringPoint> listPage(MeasureRuleDto params) {
        return page(new Page<>(params.getPageNo(), params.getPageSize()),
                new LambdaQueryWrapper<MeteringPoint>().eq(MeteringPoint::getType, params.getType())
                        .and(params.getParentId() != null, i -> i.eq(
                                        MeteringPoint::getParentId, params.getParentId())
                                .or(j -> j.eq(!Objects.equals(params.getParentId(), MeteringPoint.ROOT_ID),
                                        MeteringPoint::getId, params.getParentId()))));
    }

    /**
     * 解析公式
     */
    @Override
    public String analyticFormula(String formula) {
        if (StringUtils.isEmpty(formula)) {
            return "";
        }
        // 公式格式，中括号包裹
        // 通过正则表达式获取formula中所有被‘[]’包裹的字符串
        String regex = "\\[.*?\\]";
        Map<String, String> codeName = findCodeAndName();
        Pattern compile = Pattern.compile(regex);
        Matcher matcher = compile.matcher(formula);
        while (matcher.find()) {
            String variable = matcher.group();
            String value = codeName.getOrDefault(variable.substring(1, variable.length() - 1), "");
            formula = formula.replace(variable, value);
        }
        return formula;
    }

    /**
     * 保存公式
     */
    @Transactional
    @Override
    public boolean saveFormula(MeteringPoint rule) {

        // 获取设备信息
        // 获取点位信息
        Map<String, Device> deviceMap = deviceService.findCodeAndName()
                .stream()
                .collect(Collectors.toMap(Device::getDeviceCode, Function.identity(), (k1, k2) -> k2));
        Map<String, MeteringPoint> pointMap = list()
                .stream()
                .collect(Collectors.toMap(MeteringPoint::getNodeCode, Function.identity(), (k1, k2) -> k2));
        // 获取点位关联信息
        List<MeteringPointRel> relList = meteringPointRelService.list();
        updateFormula(rule.getId(), rule.getFormula(), deviceMap, pointMap, relList);
        return true;
    }

    private void updateFormula(Long id, String formula, Map<String, Device> deviceMap, Map<String, MeteringPoint> pointMap, List<MeteringPointRel> relList) {
        String trueFormula = formula;
        Map<Long, List<Long>> relDeviceRelMap = relList
                .stream()
                .filter(i -> i.getRelType().equals(MeteringPointRel.TYPE_DEVICE))
                .collect(Collectors.groupingBy(MeteringPointRel::getMeteringPointId,
                        Collectors.mapping(MeteringPointRel::getRelId, Collectors.toList())));
        Set<Long> pointList = new HashSet<>();
        Set<Long> deviceList = new HashSet<>();
        String regex = "\\[.*?\\]";
        Pattern compile = Pattern.compile(regex);
        Matcher matcher = compile.matcher(formula);
        while (matcher.find()) {
            String variable = matcher.group();
            Device device = deviceMap.get(variable.substring(1, variable.length() - 1));
            if (device != null) {
                deviceList.add(device.getId());
                continue;
            }
            MeteringPoint point = pointMap.get(variable.substring(1, variable.length() - 1));
            if (point != null) {
                pointList.add(point.getId());
                deviceList.addAll(relDeviceRelMap.getOrDefault(point.getId(), Collections.emptyList()));
                String pointFormula = point.getTrueFormula();
                if (StringUtils.isEmpty(pointFormula)) {
                    pointFormula = "[000]";
                }
                trueFormula = trueFormula.replace(variable, "(" + pointFormula + ")");
                continue;
            }
            // 解析失败
            throw new JeecgBootException("公式解析失败");
        }
        // 更新点位信息
        update(new LambdaUpdateWrapper<MeteringPoint>().eq(MeteringPoint::getId, id).set(MeteringPoint::getTrueFormula, trueFormula).set(MeteringPoint::getFormula, formula));
        // 更新关联关系
        meteringPointRelService.updateRel(id, deviceList, pointList);
        for (int i = relList.size() - 1; i >= 0; i--) {
            if (relList.get(i).getMeteringPointId().compareTo(id) == 0) {
                relList.remove(i);
            }
        }
        deviceList.forEach(item -> {
            relList.add(new MeteringPointRel(null, id,item, MeteringPointRel.TYPE_DEVICE));
        });
        pointList.forEach(item -> {
            relList.add(new MeteringPointRel(null, id,item, MeteringPointRel.TYPE_METERING_POINT));
        });
        // 更新pointMap
        String finalTrueFormula = trueFormula;
        pointMap.values().stream().filter(point -> point.getId().equals(id)).forEach(item -> {
            item.setFormula(formula);
            item.setTrueFormula(finalTrueFormula);
        });

        // 判断当前点位是否存在关联关系
        Set<Long> meteringPointIds = relList.stream()
                .filter(i -> i.getRelType().equals(MeteringPointRel.TYPE_METERING_POINT) && i.getRelId().compareTo(id) == 0)
                .map(MeteringPointRel::getMeteringPointId)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(meteringPointIds) || meteringPointIds.contains(id)) {
            return;
        }
        Map<Long, MeteringPoint> collect = pointMap.values().stream().collect(Collectors.toMap(MeteringPoint::getId, Function.identity(), (k1, k2) -> k2));
        for (Long l : meteringPointIds) {
            if (collect.containsKey(l)) {
                updateFormula(l, collect.get(l).getFormula(), deviceMap, pointMap, relList);
            }
        }
    }

    @Override
    public IPage<MeteringPointVo> listPoint(MeteringPointDto params) {
        params.setDeviceType(Device.DEVICE_TYPE_MEASURING);

        // 手动获取数据权限范围，传递给 Mapper 以实现权限过滤
        UserDataScope dataScope = roleDataPermissionService.getCurrentUserDataScope();

        return baseMapper.selectMeteringPoint(
            new Page<>(params.getPageNo(), params.getPageSize()),
            params,
            dataScope != null ? dataScope.getPermissionIds(RoleDataPermission.TYPE_CATEGORY) : null,
            dataScope != null ? dataScope.getPermissionIds(RoleDataPermission.TYPE_SPACE) : null
        );
    }

    @Override
    public List<MeteringPoint> listByParentId(Long parentId) {
        return list(new LambdaQueryWrapper<MeteringPoint>().eq(MeteringPoint::getParentId, parentId));
    }

    /**
     * 获取总用电规则点（运行拓扑下设备类别为电的一级节点）
     */
    @Override
    public List<MeteringPoint> getTotalElectricityConsumption() {
        return list(new LambdaQueryWrapper<MeteringPoint>()
                .eq(MeteringPoint::getType, MeteringPointConstant.TOPOLOGY_RUN)
                .eq(MeteringPoint::getCategoryId, DeviceConstant.CATEGORY_ELECTRICITY)
                .eq(MeteringPoint::getParentId, MeteringPoint.ROOT_ID)
                .isNotNull(MeteringPoint::getFormula)
        );
    }

    /**
     * 获取空间拓扑下设备类别为电的二级节点
     */
    @Override
    public List<MeteringPoint> getElectricitySecondaryNodeForSpace() {
        return getSecondaryNode(MeteringPointConstant.TOPOLOGY_SPACE, DeviceConstant.CATEGORY_ELECTRICITY);
    }

    /**
     * 获取运行拓扑下设备类别为电的二级节点
     */
    @Override
    public List<MeteringPoint> getElectricitySecondaryNodeForRun() {
        return getSecondaryNode(MeteringPointConstant.TOPOLOGY_RUN, DeviceConstant.CATEGORY_ELECTRICITY);
    }

    /**
     * 获取专业拓扑下设备类别为电的二级节点
     */
    @Override
    public List<MeteringPoint> getElectricitySecondaryForSpecialty() {
        return getSecondaryNode(MeteringPointConstant.TOPOLOGY_SPECIALTY, DeviceConstant.CATEGORY_ELECTRICITY);
    }

    /**
     * 获取专业拓扑下设备类别为电的节点
     */
    @Override
    public List<MeteringPoint> getElectricityForSpecialty() {
        return list(new LambdaQueryWrapper<MeteringPoint>().eq(MeteringPoint::getType,MeteringPointConstant.TOPOLOGY_SPECIALTY).eq(MeteringPoint::getCategoryId,DeviceConstant.CATEGORY_ELECTRICITY));
    }

    /**
     * 获取节点及节点下所有子节点
     *
     * @param id 节点id
     */
    @Override
    public List<MeteringPoint> getTreeListById(Long id) {
        if(id == null){
            return Collections.emptyList();
        }
        MeteringPoint point = super.getById(id);
        if(point == null){
            return Collections.emptyList();
        }
        List<MeteringPoint> meteringPoints = listByType(point.getType());
        if(CollectionUtils.isEmpty(meteringPoints)){
            return Collections.emptyList();
        }
        Map<Long,List<MeteringPoint>> pointMap = meteringPoints.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(MeteringPoint::getParentId,Collectors.toList()));
        // 获取id下所有子节点
        List<MeteringPoint> res = new ArrayList<>();
        res.add(point);
        for(int i = 0; i < res.size(); i++){
            MeteringPoint parentId = res.get(i);
            List<MeteringPoint> children = pointMap.get(parentId.getId());
            if(CollectionUtils.isEmpty(children)){
                continue;
            }
            res.addAll(children);
        }
        return res;
    }

    /**
     * 获取节点及节点下指定层级的子节点
     * 例：id=1,level=3,返回id=1的节点以及id=1下两级的节点
     *
     * @param id 节点id
     * @param level 层级
     */
    @Override
    public List<MeteringPoint> getTreeListByIdAndLevel(Long id, Integer level) {
        level = level == null ? 3 : level;
        MeteringPoint point = super.getById(id);
        if(point == null){
            return Collections.emptyList();
        }
        List<MeteringPoint> meteringPoints = listByType(point.getType());
        if(CollectionUtils.isEmpty(meteringPoints)){
            return Collections.emptyList();
        }
        Map<Long,List<MeteringPoint>> pointMap = meteringPoints.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(MeteringPoint::getParentId,Collectors.toList()));
        List<MeteringPoint> res = new ArrayList<>();
        res.add(point);
        List<Long> parentIds = new ArrayList<>(){{add(point.getId());}};
        while(level > 1){
            List<Long> tempIds = new ArrayList<>();
            for(Long parentId : parentIds){
                List<MeteringPoint> points = pointMap.get(parentId);
                if(CollectionUtil.isEmpty(points)){
                    continue;
                }
                res.addAll(points);
                tempIds.addAll(points.stream().map(MeteringPoint::getId).toList());
            }
            parentIds = tempIds;
            level--;
        }
        return res;
    }

    /**
     * 获取所有节点树
     */
    @Override
    public List<MeteringPointTreeVo> getAllTree() {
        List<MeteringPoint> meteringPoints = list(new LambdaQueryWrapper<MeteringPoint>().select(MeteringPoint::getId, MeteringPoint::getParentId,MeteringPoint::getNodeName,MeteringPoint::getType,MeteringPoint::getSort));
        // 构建树
        Map<Long, List<MeteringPointTreeVo>> listMap = meteringPoints
                .stream()
                .map(MeteringPointTreeVo::convert)
                .sorted(Comparator.comparing(MeteringPointTreeVo::getSort))
                .collect(Collectors.groupingBy(MeteringPointTreeVo::getParentId, Collectors.toList()));
        listMap.values().forEach(v -> {
            for (MeteringPointTreeVo item : v) {
                item.setChildren(listMap.getOrDefault(item.getId(), new ArrayList<>()));
            }
        });
        Map<String,List<MeteringPointTreeVo>> tree = listMap.getOrDefault(MeteringPoint.ROOT_ID, new ArrayList<>())
                .stream()
                .collect(Collectors.groupingBy(MeteringPointTreeVo::getType, Collectors.toList()));
        List<MeteringPointTreeVo> res = new ArrayList<>();
        // 获取字典值
        List<DictModel> dictModels = sysBaseAPI.queryDictItemsByCode(MeteringPointConstant.DICT_ENERGY_FLOW_TYPE);
        for(DictModel dict : dictModels){
            MeteringPointTreeVo item = new MeteringPointTreeVo();
            List<MeteringPointTreeVo> child = tree.get(dict.getValue());
            item.setId(-(long) dict.getValue().hashCode());
            item.setType(dict.getValue());
            item.setChildren(child);
            item.setNodeName(dict.getLabel());
            res.add(item);
        }
        return res;
    }

    /**
     * 获取点位全称
     *
     * @param id 计量规则点位id
     * @return 全称，例：专业拓扑-照明插座用点-公共照明用点
     */
    @Override
    public String getMeteringPointFullNameById(Long id) {
        MeteringPoint point = super.getById(id);
        if(point == null){
            return "";
        }
        List<String> names = new ArrayList<>();
        names.add(point.getNodeName());
        MeteringPoint parent = point;
        int i = 10;
        while(i > 0 && parent != null && parent.getParentId() != null && !parent.getParentId().equals(MeteringPoint.ROOT_ID)){
            parent = super.getById(parent.getParentId());
            if(parent != null){
                names.add(parent.getNodeName());
            }
            i--;
        }
        String typeName = sysBaseAPI.translateDict(MeteringPointConstant.DICT_ENERGY_FLOW_TYPE, point.getType());
        if(typeName != null){
            names.add(typeName);
        }
        return String.join("-", Lists.reverse(names));
    }

    /**
     * @param entity 实体对象
     * @return
     */
    @Override
    public boolean updateById(MeteringPoint entity) {
        entity.setNodeCode(null);
        entity.setType(null);
        entity.setFormula(null);
        entity.setTrueFormula(null);
        // 校验是否重复
        if(count(new LambdaQueryWrapper<MeteringPoint>().ne(MeteringPoint::getId, entity.getId()).eq(MeteringPoint::getType, entity.getType()).eq(MeteringPoint::getNodeName, entity.getNodeName())) > 0){
            throw new JeecgBootException("项目名称重复");
        }
        return super.updateById(entity);
    }


    private List<MeteringPoint> getSecondaryNode(String type, Long categoryId) {
        Map<Long, List<MeteringPoint>> meteringPointMap = listByType(type)
                .stream()
                .collect(Collectors.groupingBy(MeteringPoint::getParentId));
        // 获取一级节点
        List<Long> pointIds = meteringPointMap.getOrDefault(MeteringPoint.ROOT_ID, Collections.emptyList())
                .stream()
                .map(MeteringPoint::getId).collect(Collectors.toList());
        List<MeteringPoint> result = new ArrayList<>();
        for (Long pointId : pointIds) {
            List<MeteringPoint> meteringPoints = meteringPointMap.get(pointId);
            if (CollectionUtil.isNotEmpty(meteringPoints)) {
                meteringPoints
                        .stream()
                        .filter(i -> i.getCategoryId().equals(categoryId))
                        .forEach(result::add);
            }
        }
        return result;
    }

    private Map<String, String> findCodeAndName() {
        Map<String, String> deviceMap = deviceService.findCodeAndName().stream()
                .collect(Collectors.toMap(Device::getDeviceCode, Device::getDeviceName));
        // 获取公式的变量
        Map<String, String> pointMap = list().stream().collect(Collectors.toMap(MeteringPoint::getNodeCode, MeteringPoint::getNodeName));
        // 合并deviceMap和pointMap
        deviceMap.putAll(pointMap);
        return deviceMap;
    }

    /**
     * 生成点位编号
     * @param entity 点位信息
     * @return 点位编号
     */
    private String generateCode(MeteringPoint entity){
        Long parentId = entity.getParentId() == null ? MeteringPoint.ROOT_ID : entity.getParentId();
        return parentId + "_" + entity.getCategoryId() + "_" + entity.getSpaceId() + "_" + entity.getMeteringUnit() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0,5);
    }

}
