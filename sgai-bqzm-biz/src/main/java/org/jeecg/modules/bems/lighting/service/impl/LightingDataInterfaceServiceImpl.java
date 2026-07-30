package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingDataInterface;
import org.jeecg.modules.bems.lighting.mapper.LightingDataInterfaceMapper;
import org.jeecg.modules.bems.lighting.service.ILightingDataInterfaceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class LightingDataInterfaceServiceImpl extends ServiceImpl<LightingDataInterfaceMapper, LightingDataInterface> implements ILightingDataInterfaceService {

    @Override
    public IPage<LightingDataInterface> listPage(LightingDataInterface params, int pageNo, int pageSize) {
        LambdaQueryWrapper<LightingDataInterface> queryWrapper = new LambdaQueryWrapper<LightingDataInterface>()
                .like(StringUtils.isNotEmpty(params.getInterfaceName()), LightingDataInterface::getInterfaceName, params.getInterfaceName())
                .eq(StringUtils.isNotEmpty(params.getManufacturer()), LightingDataInterface::getManufacturer, params.getManufacturer())
                .eq(StringUtils.isNotEmpty(params.getProtocolType()), LightingDataInterface::getProtocolType, params.getProtocolType())
                .eq(StringUtils.isNotEmpty(params.getStatus()), LightingDataInterface::getStatus, params.getStatus())
                .orderByAsc(LightingDataInterface::getSort);
        return super.page(new Page<>(pageNo, pageSize), queryWrapper);
    }

    @Override
    public List<LightingDataInterface> listByStatus(String status) {
        return super.list(new LambdaQueryWrapper<LightingDataInterface>()
                .eq(LightingDataInterface::getStatus, status)
                .orderByAsc(LightingDataInterface::getSort));
    }

    @Override
    public void updateStatus(String id, String status) {
        super.update(new LambdaUpdateWrapper<LightingDataInterface>()
                .eq(LightingDataInterface::getId, id)
                .set(LightingDataInterface::getStatus, status)
                .set(LightingDataInterface::getLastSyncTime, LocalDateTime.now()));
    }
}
