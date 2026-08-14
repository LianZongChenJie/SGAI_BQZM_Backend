package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.entity.LightingDistrictAreaRel;
import org.jeecg.modules.bems.lighting.entity.LightingDistrictGroupVo;
import org.jeecg.modules.bems.lighting.mapper.LightingDistrictAreaRelMapper;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictAreaRelService;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 照明片区-分组-区域关联服务
 * <p>
 * 分组以 (district_id, group_name) 为标识，一个分组下可挂多个区域（area_id）；
 * 同一片区同一分组内一个区域只能挂一次（唯一索引 uk_dar_district_group_area 兜底）。
 */
@Slf4j
@Service
@AllArgsConstructor
public class LightingDistrictAreaRelServiceImpl extends ServiceImpl<LightingDistrictAreaRelMapper, LightingDistrictAreaRel> implements ILightingDistrictAreaRelService {

    private final ILightingAreaService areaService;

    private final ILightingDistrictService districtService;

    @Override
    public List<LightingDistrictGroupVo> listByDistrict(Long districtId) {
        if (districtId == null) {
            return Collections.emptyList();
        }
        List<LightingDistrictAreaRel> rels = list(new LambdaQueryWrapper<LightingDistrictAreaRel>()
                .eq(LightingDistrictAreaRel::getDistrictId, districtId)
                .orderByAsc(LightingDistrictAreaRel::getGroupName)
                .orderByAsc(LightingDistrictAreaRel::getSort)
                .orderByAsc(LightingDistrictAreaRel::getId));
        if (CollectionUtil.isEmpty(rels)) {
            return Collections.emptyList();
        }
        // 一次查出关联的区域，回填完整区域信息
        Set<Long> areaIds = rels.stream()
                .map(LightingDistrictAreaRel::getAreaId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, LightingArea> areaMap = CollectionUtil.isEmpty(areaIds) ? Collections.emptyMap()
                : areaService.listByIds(areaIds).stream()
                        .collect(Collectors.toMap(LightingArea::getId, Function.identity(), (a, b) -> a));
        LightingDistrict district = districtService.getById(districtId);

        // 按分组名聚合（保持查询顺序）
        Map<String, List<LightingArea>> groupMap = new LinkedHashMap<>();
        for (LightingDistrictAreaRel rel : rels) {
            LightingArea area = areaMap.get(rel.getAreaId());
            if (area == null) {
                continue;
            }
            groupMap.computeIfAbsent(rel.getGroupName(), k -> new ArrayList<>()).add(area);
        }
        List<LightingDistrictGroupVo> result = new ArrayList<>();
        for (Map.Entry<String, List<LightingArea>> entry : groupMap.entrySet()) {
            LightingDistrictGroupVo vo = new LightingDistrictGroupVo();
            vo.setDistrictId(districtId);
            vo.setDistrictName(district != null ? district.getDistrictName() : null);
            // 分组无独立表，用 districtId_groupName 作为稳定分组ID（前端唯一键）
            vo.setGroupId(districtId + "_" + entry.getKey());
            vo.setGroupName(entry.getKey());
            vo.setAreas(entry.getValue());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void addGroup(Long districtId, String groupName, List<Long> areaIds, String remark) {
        if (districtId == null) {
            throw new JeecgBootException("districtId 不能为空");
        }
        if (StringUtils.isEmpty(groupName)) {
            throw new JeecgBootException("groupName 不能为空");
        }
        if (CollectionUtil.isEmpty(areaIds)) {
            throw new JeecgBootException("areaIds 不能为空");
        }
        // 一次查出区域名称，冗余到关联表便于列表展示
        Set<Long> idSet = areaIds.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = CollectionUtil.isEmpty(idSet) ? Collections.emptyMap()
                : areaService.listByIds(idSet).stream()
                        .collect(Collectors.toMap(LightingArea::getId, LightingArea::getAreaName, (a, b) -> a));
        String operator = currentOperator();
        LocalDateTime now = LocalDateTime.now();
        int saved = 0;
        for (Long areaId : idSet) {
            // 已存在的关联跳过（不报错，保证接口可重复调用）
            long exists = count(new LambdaQueryWrapper<LightingDistrictAreaRel>()
                    .eq(LightingDistrictAreaRel::getDistrictId, districtId)
                    .eq(LightingDistrictAreaRel::getGroupName, groupName)
                    .eq(LightingDistrictAreaRel::getAreaId, areaId));
            if (exists > 0) {
                continue;
            }
            LightingDistrictAreaRel rel = new LightingDistrictAreaRel();
            rel.setDistrictId(districtId);
            rel.setGroupName(groupName);
            rel.setAreaId(areaId);
            rel.setAreaName(nameMap.get(areaId));
            rel.setRemark(remark);
            rel.setCreateBy(operator);
            rel.setCreateTime(now);
            super.save(rel);
            saved++;
        }
        log.info("【片区分组】新增分组完成：districtId={}, groupName={}, 共{}个区域，新增{}条",
                districtId, groupName, idSet.size(), saved);
    }

    @Override
    @Transactional
    public void deleteGroup(Long districtId, String groupName) {
        if (districtId == null || StringUtils.isEmpty(groupName)) {
            return;
        }
        remove(new LambdaQueryWrapper<LightingDistrictAreaRel>()
                .eq(LightingDistrictAreaRel::getDistrictId, districtId)
                .eq(LightingDistrictAreaRel::getGroupName, groupName));
        log.info("【片区分组】删除分组完成：districtId={}, groupName={}", districtId, groupName);
    }

    @Override
    @Transactional
    public void renameGroup(Long districtId, String oldName, String newName) {
        if (districtId == null) {
            throw new JeecgBootException("districtId 不能为空");
        }
        if (StringUtils.isEmpty(oldName)) {
            throw new JeecgBootException("oldName 不能为空");
        }
        if (StringUtils.isEmpty(newName)) {
            throw new JeecgBootException("newName 不能为空");
        }
        if (oldName.equals(newName)) {
            return;
        }
        // 与目标分组重名时不允许（会撞唯一索引）
        long conflict = count(new LambdaQueryWrapper<LightingDistrictAreaRel>()
                .eq(LightingDistrictAreaRel::getDistrictId, districtId)
                .eq(LightingDistrictAreaRel::getGroupName, newName));
        if (conflict > 0) {
            throw new JeecgBootException("该片区下已存在分组【" + newName + "】");
        }
        update(new LambdaUpdateWrapper<LightingDistrictAreaRel>()
                .eq(LightingDistrictAreaRel::getDistrictId, districtId)
                .eq(LightingDistrictAreaRel::getGroupName, oldName)
                .set(LightingDistrictAreaRel::getGroupName, newName)
                .set(LightingDistrictAreaRel::getUpdateBy, currentOperator())
                .set(LightingDistrictAreaRel::getUpdateTime, LocalDateTime.now()));
        log.info("【片区分组】分组重命名完成：districtId={}, {} → {}", districtId, oldName, newName);
    }

    @Override
    @Transactional
    public boolean save(LightingDistrictAreaRel entity) {
        String operator = currentOperator();
        if (entity.getCreateBy() == null) {
            entity.setCreateBy(operator);
        }
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        return super.save(entity);
    }

    @Override
    @Transactional
    public boolean updateById(LightingDistrictAreaRel entity) {
        entity.setUpdateBy(currentOperator());
        entity.setUpdateTime(LocalDateTime.now());
        return super.updateById(entity);
    }

    /**
     * 获取当前登录用户，无登录上下文时使用默认值
     */
    private String currentOperator() {
        try {
            org.jeecg.common.system.vo.LoginUser sysUser =
                    (org.jeecg.common.system.vo.LoginUser) org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
            if (sysUser != null) {
                return sysUser.getUsername();
            }
        } catch (Exception e) {
            // 无登录上下文时使用默认值
        }
        return "系统";
    }
}
