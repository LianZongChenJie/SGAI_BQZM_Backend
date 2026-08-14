package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingEnergyRead;
import org.jeecg.modules.bems.lighting.mapper.LightingEnergyReadMapper;
import org.jeecg.modules.bems.lighting.service.ILightingEnergyReadService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class LightingEnergyReadServiceImpl extends ServiceImpl<LightingEnergyReadMapper, LightingEnergyRead>
        implements ILightingEnergyReadService {

    @Override
    public boolean save(LightingEnergyRead entity) {
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        return super.save(entity);
    }
}
