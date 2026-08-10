package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.mapper.LightingAreaMapper;
import org.jeecg.modules.bems.lighting.mapper.LightingDistrictMapper;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 照明片区服务
 * <p>
 * 片区与区域一对多：片区保存 spaceIds（逗号分隔的区域空间编码集合，关联 lighting_area.space），
 * 保存/编辑/删除片区时同步维护对应区域的 district_id，保证双向一致。
 */
@Slf4j
@Service
@AllArgsConstructor
public class LightingDistrictServiceImpl extends ServiceImpl<LightingDistrictMapper, LightingDistrict> implements ILightingDistrictService {

    /**
     * 直接注入 Mapper 而非 ILightingAreaService，避免与 LightingAreaServiceImpl 形成循环依赖
     */
    private final LightingAreaMapper areaMapper;

    @Override
    @Transactional
    public boolean save(LightingDistrict entity) {
        boolean ok = super.save(entity);
        if (ok && entity.getId() != null && entity.getSpaceIds() != null) {
            // 将片区指定空间下的区域关联到该片区
            syncAreaDistrict(entity.getId(), entity.getSpaceIds());
        }
        return ok;
    }

    @Override
    @Transactional
    public boolean updateById(LightingDistrict entity) {
        boolean ok = super.updateById(entity);
        // 只有前端显式传了 spaceIds 才做关联同步（不传则视为部分更新，不动区域归属）
        if (ok && entity.getId() != null && entity.getSpaceIds() != null) {
            // 原片区下所有区域先解除关联
            clearAreaDistrict(entity.getId());
            // 新指定空间下的区域建立关联
            syncAreaDistrict(entity.getId(), entity.getSpaceIds());
        }
        return ok;
    }

    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        boolean ok = super.removeById(id);
        if (ok) {
            // 删除片区时，其下所有区域解除片区关联
            areaMapper.update(null, new LambdaUpdateWrapper<LightingArea>()
                    .eq(LightingArea::getDistrictId, id)
                    .set(LightingArea::getDistrictId, null));
        }
        return ok;
    }

    /**
     * 将 spaceIds（逗号分隔的空间编码）下所有区域的 districtId 更新为当前片区
     */
    private void syncAreaDistrict(Long districtId, String spaceIds) {
        List<String> spaces = parseSpaces(spaceIds);
        if (spaces.isEmpty()) {
            return;
        }
        areaMapper.update(null, new LambdaUpdateWrapper<LightingArea>()
                .in(LightingArea::getSpace, spaces)
                .set(LightingArea::getDistrictId, districtId));
    }

    /**
     * 解除指定片区下所有区域的片区关联
     */
    private void clearAreaDistrict(Long districtId) {
        areaMapper.update(null, new LambdaUpdateWrapper<LightingArea>()
                .eq(LightingArea::getDistrictId, districtId)
                .set(LightingArea::getDistrictId, null));
    }

    /**
     * 解析逗号分隔的空间编码字符串（去空格、去空、去重）
     */
    private List<String> parseSpaces(String spaceIds) {
        if (StringUtils.isEmpty(spaceIds)) {
            return Collections.emptyList();
        }
        return Arrays.stream(spaceIds.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
    }
}
