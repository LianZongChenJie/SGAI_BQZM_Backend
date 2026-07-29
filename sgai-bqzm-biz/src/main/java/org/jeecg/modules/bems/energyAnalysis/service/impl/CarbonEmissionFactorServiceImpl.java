package org.jeecg.modules.bems.energyAnalysis.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.energyAnalysis.constant.EnergyAnalysisConstant;
import org.jeecg.modules.bems.energyAnalysis.entity.CarbonEmissionFactor;
import org.jeecg.modules.bems.energyAnalysis.mapper.CarbonEmissionFactorMapper;
import org.jeecg.modules.bems.energyAnalysis.service.ICarbonEmissionFactorService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * @Description: 碳排放因子管理
 * @Author: jeecg-boot
 * @Date:   2025-03-05
 * @Version: V1.0
 */
@Service
public class CarbonEmissionFactorServiceImpl extends ServiceImpl<CarbonEmissionFactorMapper, CarbonEmissionFactor> implements ICarbonEmissionFactorService {


    /**
     * 获取电-碳排放因子
     */
    @Override
    public BigDecimal getElectricityCarbonEmissionFactor() {
        List<CarbonEmissionFactor> list = list(new LambdaQueryWrapper<CarbonEmissionFactor>().eq(CarbonEmissionFactor::getCarbonFactorName, "电"));
        if(CollectionUtil.isEmpty(list) || list.get(0) == null || StringUtils.isEmpty(list.get(0).getCoefficient())){
            return EnergyAnalysisConstant.CARBON_EMISSION_FACTOR;
        }
        return new BigDecimal(list.get(0).getCoefficient());
    }
}
