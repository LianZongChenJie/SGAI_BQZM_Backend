package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.mapper.LightingDistrictMapper;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.springframework.stereotype.Service;

@Service
public class LightingDistrictServiceImpl extends ServiceImpl<LightingDistrictMapper, LightingDistrict> implements ILightingDistrictService {
}
