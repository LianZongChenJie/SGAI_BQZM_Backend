package org.jeecg.modules.bems.energyAnalysis.service;

import org.jeecg.modules.bems.energyAnalysis.entity.CarbonEmissionFactor;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

/**
 * @Description: 碳排放因子管理
 * @Author: jeecg-boot
 * @Date:   2025-03-05
 * @Version: V1.0
 */
public interface ICarbonEmissionFactorService extends IService<CarbonEmissionFactor> {
    /**
     * 获取电-碳排放因子
     */
    BigDecimal getElectricityCarbonEmissionFactor();
}
