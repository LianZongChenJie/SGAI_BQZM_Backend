package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.energyAnalysis.entity.CostCenterRel;
import org.jeecg.modules.bems.energyAnalysis.mapper.CostCenterRelMapper;
import org.jeecg.modules.bems.energyAnalysis.service.ICostCenterRelService;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CostCenterRelServiceImpl extends ServiceImpl<CostCenterRelMapper, CostCenterRel> implements ICostCenterRelService {
    @Transactional
    @Override
    public void saveRel(Long costCenterId, List<MeteringPointVo> relList) {
        remove(new LambdaQueryWrapper<CostCenterRel>().eq(CostCenterRel::getCostCenterId, costCenterId));
        for(MeteringPointVo meteringPointVo : relList){
            CostCenterRel costCenterRel = new CostCenterRel();
            costCenterRel.setCostCenterId(costCenterId);
            costCenterRel.setRelId(meteringPointVo.getPointId());
            costCenterRel.setPointName(meteringPointVo.getPointName());
            costCenterRel.setPointCode(meteringPointVo.getPointCode());
            costCenterRel.setCategoryId(meteringPointVo.getCategoryId());
            costCenterRel.setSpaceId(meteringPointVo.getSpaceId());
            costCenterRel.setRelType(meteringPointVo.getPointType());
            save(costCenterRel);
        }
    }

    @Override
    public List<CostCenterRel> listByCostCenterId(Long costCenterId) {
        return list(new LambdaQueryWrapper<CostCenterRel>().eq(CostCenterRel::getCostCenterId, costCenterId));
    }


    /**
     * 判断关联关系id是否存在
     *
     * @param type  关联关系类型
     * @param relId 关联关系id
     * @return 存在：true；不存在：false
     */
    @Override
    public boolean checkRelId(String type, Long relId) {
        return count(new LambdaQueryWrapper<CostCenterRel>().eq(CostCenterRel::getRelType, type).eq(CostCenterRel::getRelId, relId)) > 0;
    }

    /**
     * 判断计量点id是否存在关联关系
     * @param meteringPointId 计量点id
     * @return 存在：true；不存在：false
     */
    @Override
    public boolean checkMeteringPointId(Long meteringPointId) {
        return count(new LambdaQueryWrapper<CostCenterRel>().eq(CostCenterRel::getRelType,CostCenterRel.REL_TYPE_METERING_POINT).eq(CostCenterRel::getRelId, meteringPointId)) > 0;
    }
}
